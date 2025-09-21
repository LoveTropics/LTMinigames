package com.lovetropics.minigames.common.core.game.behavior.instances.statistics;

import com.lovetropics.minigames.common.content.MinigameTexts;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameLogicEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.state.statistics.Placement;
import com.lovetropics.minigames.common.core.game.state.statistics.PlacementOrder;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record DisplayLeaderboardOnFinishBehavior<T extends Comparable<T>>(StatisticKey<T> statistic, PlacementOrder order, int length) implements IGameBehavior {
	public static final MapCodec<DisplayLeaderboardOnFinishBehavior<?>> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			StatisticKey.CODEC.fieldOf("statistic").forGetter(c -> c.statistic),
			PlacementOrder.CODEC.optionalFieldOf("order", PlacementOrder.MAX).forGetter(c -> c.order),
			Codec.INT.optionalFieldOf("length", 5).forGetter(c -> c.length)
	).apply(i, DisplayLeaderboardOnFinishBehavior::createUnchecked));

	@SuppressWarnings("unchecked")
	private static <T extends Comparable<T>> DisplayLeaderboardOnFinishBehavior<T> createUnchecked(StatisticKey<?> statistic, PlacementOrder order, int length) {
		return new DisplayLeaderboardOnFinishBehavior<>((StatisticKey<T>) statistic, order, length);
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		TeamState teams = game.instanceState().getOrNull(TeamState.KEY);
		events.listen(GameLogicEvents.GAME_OVER, winner -> {
			PlayerSet players = game.allPlayers();
			players.sendMessage(MinigameTexts.RESULTS);
			if (teams == null) {
				Placement.fromPlayerScore(order, game, statistic).sendTo(players, length);
			} else {
				Placement.fromTeamScore(order, game, statistic).sendTo(players, length);
			}
		});
	}
}
