package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.function.Supplier;

public record IncrementStatisticAction(
		StatisticKey<Integer> statistic,
		int amount,
		SetStatisticAction.Scope scope
) implements IGameBehavior {
	public static final MapCodec<IncrementStatisticAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			StatisticKey.typedCodec(Integer.class).fieldOf("statistic").forGetter(IncrementStatisticAction::statistic),
			Codec.INT.optionalFieldOf("amount", 1).forGetter(IncrementStatisticAction::amount),
			SetStatisticAction.Scope.CODEC.fieldOf("scope").forGetter(IncrementStatisticAction::scope)
	).apply(i, IncrementStatisticAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GameActionEvents.APPLY, (context, targets) ->
				scope.applyTo(game, targets, statistics -> statistics.incrementInt(statistic, amount))
		);
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.INCREMENT_STATISTIC;
	}
}
