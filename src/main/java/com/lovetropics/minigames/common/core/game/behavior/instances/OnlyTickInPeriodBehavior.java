package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameEventListeners;
import com.lovetropics.minigames.common.core.game.behavior.event.GameLivingEntityEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.state.GameStateMap;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressChannel;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressionPeriod;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public record OnlyTickInPeriodBehavior(ProgressChannel channel, ProgressionPeriod period, IGameBehavior behavior) implements IGameBehavior {
	public static final MapCodec<OnlyTickInPeriodBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ProgressChannel.CODEC.optionalFieldOf("channel", ProgressChannel.MAIN).forGetter(OnlyTickInPeriodBehavior::channel),
			ProgressionPeriod.CODEC.fieldOf("period").forGetter(OnlyTickInPeriodBehavior::period),
			IGameBehavior.CODEC.fieldOf("behavior").forGetter(OnlyTickInPeriodBehavior::behavior)
	).apply(i, OnlyTickInPeriodBehavior::new));

	@Override
	public void registerState(final IGamePhase game, final GameStateMap phaseState, final GameStateMap instanceState) {
		behavior.registerState(game, phaseState, instanceState);
	}

	@Override
	public void register(final IGamePhase game, final EventRegistrar events) {
		final GameEventListeners conditionalEvents = new GameEventListeners();
		behavior.register(game, events.redirect(type -> type == GamePhaseEvents.TICK || type == GamePlayerEvents.TICK || type == GameLivingEntityEvents.TICK, conditionalEvents));

		final BooleanSupplier predicate = period.createPredicate(game, channel);
		if (conditionalEvents.hasListeners(GamePhaseEvents.TICK)) {
			events.listen(GamePhaseEvents.TICK, () -> {
				if (predicate.getAsBoolean()) {
					conditionalEvents.invoker(GamePhaseEvents.TICK).tick();
				}
			});
		}

		if (conditionalEvents.hasListeners(GamePlayerEvents.TICK)) {
			events.listen(GamePlayerEvents.TICK, player -> {
				if (predicate.getAsBoolean()) {
					conditionalEvents.invoker(GamePlayerEvents.TICK).tick(player);
				}
			});
		}

		if (conditionalEvents.hasListeners(GameLivingEntityEvents.TICK)) {
			events.listen(GameLivingEntityEvents.TICK, (level, entity) -> {
				if (predicate.getAsBoolean()) {
					conditionalEvents.invoker(GameLivingEntityEvents.TICK).tick(level, entity);
				}
			});
		}
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.ONLY_TICK_IN_PERIOD;
	}
}
