package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.mojang.serialization.Codec;
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
import org.lovetropics.games.common.core.game.state.statistics.Placement;
import org.lovetropics.games.common.core.game.state.statistics.PlacementOrder;
import org.lovetropics.games.common.core.game.state.statistics.PlayerKey;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;
import org.lovetropics.games.common.core.game.state.team.GameTeamKey;
import org.lovetropics.games.common.core.game.state.team.TeamState;

public record DisplayLeaderboardAction<T extends Comparable<T>>(
		StatisticKey<T> statistic,
		PlacementOrder order,
		int length,
		Component header,
		boolean showPlacement,
		boolean showScore
) implements IGameBehavior {
	public static final MapCodec<DisplayLeaderboardAction<?>> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			StatisticKey.CODEC.fieldOf("statistic").forGetter(c -> c.statistic),
			PlacementOrder.CODEC.optionalFieldOf("order", PlacementOrder.MAX).forGetter(c -> c.order),
			Codec.INT.optionalFieldOf("length", 5).forGetter(c -> c.length),
			ComponentSerialization.CODEC.fieldOf("header").forGetter(c -> c.header),
			Codec.BOOL.optionalFieldOf("show_placement", true).forGetter(c -> c.showPlacement),
			Codec.BOOL.optionalFieldOf("show_score", true).forGetter(c -> c.showScore)
	).apply(i, DisplayLeaderboardAction::createUnchecked));

	@SuppressWarnings("unchecked")
	private static <T extends Comparable<T>> DisplayLeaderboardAction<T> createUnchecked(StatisticKey<?> statistic, PlacementOrder order, int length, Component header, boolean showPlacement, boolean showScore) {
		return new DisplayLeaderboardAction<>((StatisticKey<T>) statistic, order, length, header, showPlacement, showScore);
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		TeamState teams = game.instanceState().getOrNull(TeamState.KEY);
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			PlayerIterable players = PlayerIterable.from(targets.asPlayers(game));
			players.sendMessage(header);
			if (teams == null) {
				Placement.Score<PlayerKey, T> placement = Placement.fromPlayerScore(order, game, statistic);
				placement.buildDisplay(length, showPlacement, showScore, players::sendMessage);
				for (Placement.Score.Entry<PlayerKey, T> entry : placement.entries()) {
					ServerPlayer player = game.allPlayers().getPlayerBy(entry.holder());
					if (player != null && entry.placement() > length) {
						sendPersonalResult(entry, PlayerSet.of(player));
					}
				}
			} else {
				Placement.Score<GameTeamKey, T> placement = Placement.fromTeamScore(order, game, statistic);
				placement.buildDisplay(length, showPlacement, showScore, players::sendMessage);
				for (Placement.Score.Entry<GameTeamKey, T> entry : placement.entries()) {
					if (entry.placement() > length) {
						sendPersonalResult(entry, teams.getPlayersForTeam(game, entry.holder()));
					}
				}
			}
			return true;
		});
	}

	private void sendPersonalResult(Placement.Score.Entry<?, T> entry, PlayerIterable players) {
		Component placement = Component.literal("#" + entry.placement()).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
		if (showScore) {
			Component display = Component.literal(statistic.display(entry.score())).withStyle(ChatFormatting.GOLD);
			players.sendMessage(MinigameTexts.PERSONAL_RESULT_SCORE.apply(placement, display));
		} else {
			players.sendMessage(MinigameTexts.PERSONAL_RESULT.apply(placement));
		}
	}
}
