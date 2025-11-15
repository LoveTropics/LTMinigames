package com.lovetropics.minigames.common.core.game.behavior.instances.trigger;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressChannel;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressionPeriod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.context.ContextMap;

import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public record PeriodicActionsTrigger(ProgressChannel channel, Optional<ProgressionPeriod> inPeriod, int interval, GameActionList<Void> actions) implements IGameBehavior {
	public static final MapCodec<PeriodicActionsTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ProgressChannel.CODEC.optionalFieldOf("channel", ProgressChannel.MAIN).forGetter(PeriodicActionsTrigger::channel),
			ProgressionPeriod.CODEC.optionalFieldOf("in_period").forGetter(PeriodicActionsTrigger::inPeriod),
			Codec.INT.fieldOf("interval").forGetter(PeriodicActionsTrigger::interval),
			GameActionList.VOID_CODEC.fieldOf("actions").forGetter(PeriodicActionsTrigger::actions)
	).apply(i, PeriodicActionsTrigger::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		actions.register(game, events);

		BooleanSupplier isActive = inPeriod.map(period -> period.createPredicate(game, channel)).orElse(() -> true);
		int onTick = interval - 1;
		events.listen(GamePhaseEvents.TICK, () -> {
			if (game.ticks() % interval == onTick && isActive.getAsBoolean()) {
				actions.apply(game, ContextMap.EMPTY);
			}
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.PERIODIC_ACTIONS;
	}
}
