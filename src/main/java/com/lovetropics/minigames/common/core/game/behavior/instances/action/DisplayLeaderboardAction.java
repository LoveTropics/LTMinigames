package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerIterable;
import com.lovetropics.minigames.common.core.game.state.statistics.Placement;
import com.lovetropics.minigames.common.core.game.state.statistics.PlacementOrder;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

public record DisplayLeaderboardAction<T extends Comparable<T>>(StatisticKey<T> statistic, PlacementOrder order, int length, Component header) implements IGameBehavior {
	public static final MapCodec<DisplayLeaderboardAction<?>> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			StatisticKey.CODEC.fieldOf("statistic").forGetter(c -> c.statistic),
			PlacementOrder.CODEC.optionalFieldOf("order", PlacementOrder.MAX).forGetter(c -> c.order),
			Codec.INT.optionalFieldOf("length", 5).forGetter(c -> c.length),
			ComponentSerialization.CODEC.fieldOf("header").forGetter(c -> c.header)
	).apply(i, DisplayLeaderboardAction::createUnchecked));

	@SuppressWarnings("unchecked")
	private static <T extends Comparable<T>> DisplayLeaderboardAction<T> createUnchecked(StatisticKey<?> statistic, PlacementOrder order, int length, Component header) {
		return new DisplayLeaderboardAction<>((StatisticKey<T>) statistic, order, length, header);
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		TeamState teams = game.instanceState().getOrNull(TeamState.KEY);
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			PlayerIterable players = PlayerIterable.from(targets.asPlayers(game));
			players.sendMessage(header);
			if (teams == null) {
				Placement.fromPlayerScore(order, game, statistic).sendTo(players, length);
			} else {
				Placement.fromTeamScore(order, game, statistic).sendTo(players, length);
			}
			return true;
		});
	}
}
