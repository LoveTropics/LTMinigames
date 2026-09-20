package org.lovetropics.games.common.core.game.behavior.instances;

import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorType;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorTypes;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameEventListeners;
import org.lovetropics.games.common.core.game.behavior.event.GameLivingEntityEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.state.GameStateMap;
import org.lovetropics.games.common.core.game.state.progress.ProgressChannel;
import org.lovetropics.games.common.core.game.state.progress.ProgressionPeriod;
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
	public void registerState(IGamePhase game, GameStateMap phaseState, GameStateMap instanceState) {
		behavior.registerState(game, phaseState, instanceState);
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		GameEventListeners conditionalEvents = new GameEventListeners();
		behavior.register(game, events.redirect(type -> type == GamePhaseEvents.TICK || type == GamePlayerEvents.TICK || type == GameLivingEntityEvents.TICK, conditionalEvents));

		BooleanSupplier predicate = period.createPredicate(game, channel);
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
