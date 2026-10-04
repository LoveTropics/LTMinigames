package org.lovetropics.games.common.content.mcsr.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.EntityTypes;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.content.MinigameTexts;
import org.lovetropics.games.common.content.bingo.Bingo;
import org.lovetropics.games.common.content.mcsr.McsrTexts;
import org.lovetropics.games.common.core.game.GameWinner;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameLivingEntityEvents;
import org.lovetropics.games.common.core.game.behavior.event.GameLogicEvents;
import org.lovetropics.games.common.core.game.behavior.event.SubGameEvents;
import org.lovetropics.games.common.core.game.player.PlayerRole;
import org.lovetropics.games.common.core.game.player.PlayerSet;
import org.lovetropics.games.common.core.game.state.statistics.GameStatistics;
import org.lovetropics.games.common.core.game.state.statistics.PlayerKey;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;
import org.lovetropics.games.common.core.game.util.TranslationCollector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/// Ends the game as soon as a player finishes the game, by killing the Ender Dragon or by completing a whole bingo card.
/// The finisher places first, and everyone else is placed by their points.
///
/// If nobody finishes before the game is ended otherwise (e.g. by the time limit), everyone is placed by their points.
///
/// Must be in the same phase as the bingo board, as that is where the points are kept.
public final class FinishBehavior implements IGameBehavior {
	public static final MapCodec<FinishBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.BOOL.optionalFieldOf("dragon_kill", true).forGetter(b -> b.dragonKill),
			Codec.BOOL.optionalFieldOf("full_bnigo_card", true).forGetter(b -> b.fullBingoCard)
	).apply(i, FinishBehavior::new));

	private final boolean dragonKill;
	private final boolean fullBingoCard;
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
			events.listen(Bingo.BOARD_COMPLETED, player -> finish(game, player, McsrTexts.COMPLETED_BOARD));
		}

		if (dragonKill) {
			events.listen(SubGameEvents.CREATE, (world, worldEvents) -> worldEvents.listen(GameLivingEntityEvents.DEATH, (level, entity, damageSource) -> {
				if (entity.getType() == EntityTypes.ENDER_DRAGON) {
					// The dragon is usually killed with beds, which don't credit anyone with the kill, so credit whoever owns the world
					ServerPlayer owner = getParticipant(game, world.allPlayers());
					if (owner != null) {
						finish(game, owner, McsrTexts.KILLED_DRAGON);
					}
				}
				return TriState.DEFAULT;
			}));
		}

		events.listen(GameLogicEvents.REQUEST_GAME_OVER, () -> {
			if (!over) {
				game.allPlayers(true).sendMessage(McsrTexts.TIME_UP);
				endGame(game, null);
			}
			return true;
		});
	}

	private static @Nullable ServerPlayer getParticipant(IGamePhase game, PlayerSet players) {
		for (ServerPlayer player : players) {
			if (game.getRoleFor(player) == PlayerRole.PARTICIPANT) {
				return player;
			}
		}
		return null;
	}

	private void finish(IGamePhase game, ServerPlayer player, TranslationCollector.Fun1 message) {
		if (over) {
			return;
		}
		game.allPlayers(true).sendMessage(message.apply(player.getDisplayName()).withStyle(ChatFormatting.GOLD));
		endGame(game, player);
	}

	private void endGame(IGamePhase game, @Nullable ServerPlayer finisher) {
		over = true;

		List<PlayerKey> ranking = rank(game, finisher);
		GameStatistics statistics = game.statistics();
		List<Component> results = new ArrayList<>();
		results.add(MinigameTexts.RESULTS);

		int placement = 0;
		Integer lastPoints = null;
		for (PlayerKey player : ranking) {
			int points = getPoints(statistics, player);
			boolean finished = finisher != null && player.id().equals(finisher.getUUID());
			// Players with the same points share a placement, but the finisher is first on their own
			if (lastPoints == null || points != lastPoints) {
				placement++;
			}
			lastPoints = finished ? null : points;
			statistics.forPlayer(player).set(StatisticKey.PLACEMENT, placement);

			Component name = Component.literal(player.name()).withStyle(ChatFormatting.AQUA);
			results.add(finished ? McsrTexts.RESULT_FINISHED.apply(placement, name, points) : McsrTexts.RESULT.apply(placement, name, points));
		}

		for (Component line : results) {
			game.allPlayers(true).sendMessage(line);
		}

		game.invoker(GameLogicEvents.GAME_OVER).onGameOver(getWinner(game, statistics, ranking, finisher));
	}

	private static List<PlayerKey> rank(IGamePhase game, @Nullable ServerPlayer finisher) {
		GameStatistics statistics = game.statistics();

		// Players who left the game still keep their points
		Set<PlayerKey> players = new LinkedHashSet<>();
		for (PlayerKey player : statistics.getPlayers()) {
			if (statistics.forPlayer(player).get(StatisticKey.POINTS) != null) {
				players.add(player);
			}
		}
		for (ServerPlayer player : game.allPlayers(true)) {
			if (game.getRoleFor(player) == PlayerRole.PARTICIPANT) {
				players.add(PlayerKey.from(player));
			}
		}

		List<PlayerKey> ranking = new ArrayList<>(players);
		ranking.sort(Comparator.comparing((PlayerKey player) -> finisher != null && player.id().equals(finisher.getUUID()))
				.thenComparingInt(player -> getPoints(statistics, player))
				.reversed());
		return ranking;
	}

	private static GameWinner getWinner(IGamePhase game, GameStatistics statistics, List<PlayerKey> ranking, @Nullable ServerPlayer finisher) {
		if (finisher != null) {
			return new GameWinner.Player(finisher);
		}
		// Nobody wins a tie for the most points
		if (ranking.isEmpty() || ranking.size() > 1 && getPoints(statistics, ranking.get(0)) == getPoints(statistics, ranking.get(1))) {
			return new GameWinner.Nobody();
		}
		ServerPlayer winner = game.allPlayers(true).getPlayerBy(ranking.getFirst().id());
		return winner != null ? new GameWinner.Player(winner) : new GameWinner.OfflinePlayer(ranking.getFirst(), Component.literal(ranking.getFirst().name()));
	}

	private static int getPoints(GameStatistics statistics, PlayerKey player) {
		return statistics.forPlayer(player).getOr(StatisticKey.POINTS, 0);
	}
}
