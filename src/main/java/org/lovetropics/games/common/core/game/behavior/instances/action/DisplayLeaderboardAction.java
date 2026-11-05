package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerPlayer;
import org.lovetropics.games.common.content.MinigameTexts;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.core.game.player.PlayerIterable;
import org.lovetropics.games.common.core.game.player.PlayerSet;
import org.lovetropics.games.common.core.game.state.statistics.LeaderboardConfig;
import org.lovetropics.games.common.core.game.state.statistics.Placement;
import org.lovetropics.games.common.core.game.state.statistics.PlayerKey;
import org.lovetropics.games.common.core.game.state.team.GameTeamKey;
import org.lovetropics.games.common.core.game.state.team.TeamState;

public record DisplayLeaderboardAction(
		Component header,
		LeaderboardConfig<?> leaderboard
) implements IGameBehavior {
	public static final MapCodec<DisplayLeaderboardAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ComponentSerialization.CODEC.fieldOf("header").forGetter(c -> c.header),
			LeaderboardConfig.MAP_CODEC.forGetter(DisplayLeaderboardAction::leaderboard)
	).apply(i, DisplayLeaderboardAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			PlayerSet targetPlayers = PlayerSet.copyOf(targets.asPlayers(game));
			targetPlayers.sendMessage(header);
			if (leaderboard.shouldPlaceByTeam(game)) {
				TeamState teams = game.instanceState().getOrThrow(TeamState.KEY);
				sendByTeam(game, teams, targetPlayers, leaderboard);
			} else {
				sendByPlayer(game, targetPlayers, leaderboard);
			}
			return true;
		});
	}

	private <T extends Comparable<T>> void sendByPlayer(IGamePhase game, PlayerSet targetPlayers, LeaderboardConfig<T> leaderboard) {
		Placement.Score<PlayerKey, T> placement = Placement.fromPlayerScore(leaderboard.order(), game, game.statistics(), leaderboard.statistic());
		placement.buildDisplay(leaderboard.length(), leaderboard.showPlacement(), leaderboard.showScore(), targetPlayers::sendMessage);
		for (Placement.Score.Entry<PlayerKey, T> entry : placement.entries()) {
			ServerPlayer player = targetPlayers.getPlayerBy(entry.holder());
			if (player != null && entry.placement() > leaderboard.length()) {
				sendPersonalResult(entry, PlayerSet.of(player), leaderboard);
			}
		}
	}

	private <T extends Comparable<T>> void sendByTeam(IGamePhase game, TeamState teams, PlayerSet targetPlayers, LeaderboardConfig<T> leaderboard) {
		Placement.Score<GameTeamKey, T> placement = Placement.fromTeamScore(leaderboard.order(), game, game.statistics(), leaderboard.statistic());
		placement.buildDisplay(leaderboard.length(), leaderboard.showPlacement(), leaderboard.showScore(), targetPlayers::sendMessage);
		for (Placement.Score.Entry<GameTeamKey, T> entry : placement.entries()) {
			if (entry.placement() > leaderboard.length()) {
				sendPersonalResult(entry, PlayerSet.intersection(targetPlayers, teams.getPlayersForTeam(game, entry.holder())), leaderboard);
			}
		}
	}

	private <T extends Comparable<T>> void sendPersonalResult(Placement.Score.Entry<?, T> entry, PlayerIterable players, LeaderboardConfig<T> leaderboard) {
		Component placement = Component.literal("#" + entry.placement()).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
		if (leaderboard.showScore()) {
			Component display = Component.literal(leaderboard.statistic().display(entry.score())).withStyle(ChatFormatting.GOLD);
			players.sendMessage(MinigameTexts.PERSONAL_RESULT_SCORE.apply(placement, display));
		} else {
			players.sendMessage(MinigameTexts.PERSONAL_RESULT.apply(placement));
		}
	}
}
