package org.lovetropics.games.common.content.tropic_rush.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.EntityTypes;
import org.lovetropics.games.common.content.MinigameTexts;
import org.lovetropics.games.common.content.bingo.Bingo;
import org.lovetropics.games.common.content.tropic_rush.TropicRushTexts;
import org.lovetropics.games.common.core.game.GameWinner;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameLivingEntityEvents;
import org.lovetropics.games.common.core.game.behavior.event.GameLogicEvents;
import org.lovetropics.games.common.core.game.player.PlayerRole;
import org.lovetropics.games.common.core.game.state.statistics.GameStatistics;
import org.lovetropics.games.common.core.game.state.statistics.PlayerKey;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;
import org.lovetropics.games.common.core.game.util.TranslationCollector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/// Players finish the game by killing the Ender Dragon or by completing a whole bingo card, which makes them spectators (ghosts).
/// The game ends once nobody is left playing, or when it is ended otherwise (e.g. by the time limit).
///
/// Finishers place first in the order they finished, and everyone else is placed by their points.
///
/// Must be in the same phase as the bingo board, as that is where the points are kept.
public final class FinishBehavior implements IGameBehavior {
	public static final MapCodec<FinishBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.BOOL.optionalFieldOf("dragon_kill", true).forGetter(b -> b.dragonKill),
			Codec.BOOL.optionalFieldOf("full_bingo_card", true).forGetter(b -> b.fullBingoCard)
	).apply(i, FinishBehavior::new));

	private final boolean dragonKill;
	private final boolean fullBingoCard;
	private final List<PlayerKey> finishers = new ArrayList<>();
	private boolean over;

	/// @param dragonKill whether killing the Ender Dragon finishes the game
	/// @param fullBingoCard   whether completing every tile of the bingo card finishes the game
	public FinishBehavior(boolean dragonKill, boolean fullBingoCard) {
		this.dragonKill = dragonKill;
		this.fullBingoCard = fullBingoCard;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		if (fullBingoCard) {
			events.listen(Bingo.BOARD_COMPLETED, player -> finish(game, player, TropicRushTexts.COMPLETED_BOARD));
		}

		if (dragonKill) {
			events.listen(GameLivingEntityEvents.DEATH, (level, entity, damageSource) -> {
				if (entity.getType() == EntityTypes.ENDER_DRAGON) {
					// The dragon is usually killed with beds, which don't credit anyone with the kill, so credit whoever owns the world
					game.participants().inLevel(level).stream().findAny()
							.ifPresent(owner -> finish(game, owner, TropicRushTexts.KILLED_DRAGON));
				}
				return TriState.DEFAULT;
			});
		}

		events.listen(GameLogicEvents.REQUEST_GAME_OVER, () -> {
			if (!over) {
				game.allPlayers().sendMessage(TropicRushTexts.TIME_UP);
				endGame(game);
			}
			return true;
		});
	}

	private void finish(IGamePhase game, ServerPlayer player, TranslationCollector.Fun1 message) {
		PlayerKey playerKey = PlayerKey.from(player);
		if (over || finishers.contains(playerKey)) {
			return;
		}
		finishers.add(playerKey);
		game.allPlayers().sendMessage(message.apply(player.getDisplayName()).withStyle(ChatFormatting.GOLD));

		// We may be in the middle of ticking the player or their level, so don't move them away just yet
		game.scheduler().execute(() -> {
			if (over) {
				return;
			}
			game.setPlayerRole(player, PlayerRole.SPECTATOR);
			if (game.participants().isEmpty()) {
				endGame(game);
			}
		});
	}

	private void endGame(IGamePhase game) {
		over = true;

		List<PlayerKey> ranking = rank(game);
		GameStatistics statistics = game.statistics();
		List<Component> results = new ArrayList<>();
		results.add(MinigameTexts.RESULTS);

		int placement = 0;
		Integer lastPoints = null;
		for (PlayerKey player : ranking) {
			int points = getPoints(statistics, player);
			boolean finished = finishers.contains(player);
			// Players with the same points share a placement, but finishers are on their own
			if (lastPoints == null || points != lastPoints) {
				placement++;
			}
			lastPoints = finished ? null : points;
			statistics.forPlayer(player).set(StatisticKey.PLACEMENT, placement);

			Component name = Component.literal(player.name()).withStyle(ChatFormatting.AQUA);
			results.add(finished ? TropicRushTexts.RESULT_FINISHED.apply(placement, name, points) : TropicRushTexts.RESULT.apply(placement, name, points));
		}

		for (Component line : results) {
			game.allPlayers().sendMessage(line);
		}

		game.invoker(GameLogicEvents.GAME_OVER).onGameOver(getWinner(game, statistics, ranking));
	}

	private List<PlayerKey> rank(IGamePhase game) {
		GameStatistics statistics = game.statistics();

		// Players who left the game still keep their points
		Set<PlayerKey> players = new LinkedHashSet<>(finishers);
		for (PlayerKey player : statistics.getPlayers()) {
			if (statistics.readForPlayer(player).get(StatisticKey.POINTS) != null) {
				players.add(player);
			}
		}
		for (ServerPlayer player : game.participants()) {
			players.add(PlayerKey.from(player));
		}

		List<PlayerKey> ranking = new ArrayList<>(players);
		ranking.sort(Comparator.<PlayerKey>comparingInt(this::getFinishOrder)
				.thenComparing(Comparator.comparingInt((PlayerKey player) -> getPoints(statistics, player)).reversed()));
		return ranking;
	}

	/// @return when the given player finished, with players who didn't finish coming last
	private int getFinishOrder(PlayerKey player) {
		int order = finishers.indexOf(player);
		return order != -1 ? order : finishers.size();
	}

	private GameWinner getWinner(IGamePhase game, GameStatistics statistics, List<PlayerKey> ranking) {
		// Nobody wins a tie for the most points, if nobody finished
		if (finishers.isEmpty() && (ranking.isEmpty() || ranking.size() > 1 && getPoints(statistics, ranking.get(0)) == getPoints(statistics, ranking.get(1)))) {
			return new GameWinner.Nobody();
		}
		ServerPlayer winner = game.allPlayers().getPlayerBy(ranking.getFirst().id());
		return winner != null ? new GameWinner.Player(winner) : new GameWinner.OfflinePlayer(ranking.getFirst(), Component.literal(ranking.getFirst().name()));
	}

	private static int getPoints(GameStatistics statistics, PlayerKey player) {
		return statistics.readForPlayer(player).getOr(StatisticKey.POINTS, 0);
	}
}
