package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionSubjects;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.context.ContextMap;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;

import java.util.Optional;

public record DelayedAction(
		IntProvider delay,
		Optional<Integer> initialDelay,
		int repetitions,
		GameActionList actions
) implements IGameBehavior {
	public static final MapCodec<DelayedAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			IntProviders.POSITIVE_CODEC.fieldOf("delay").forGetter(b -> b.delay),
			ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("initial_delay").forGetter(b -> b.initialDelay),
			ExtraCodecs.POSITIVE_INT.optionalFieldOf("repetitions", 1).forGetter(b -> b.repetitions),
			GameActionList.MAP_CODEC.forGetter(c -> c.actions)
	).apply(i, DelayedAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		actions.register(game, events);

		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			schedule(game, context, targets, 0);
			return true;
		});
	}

	private void schedule(IGamePhase game, ContextMap context, ActionSubjects<?> targets, int repeatedCount) {
		if (repeatedCount >= repetitions) {
			return;
		}
		int ticks = delay.sample(game.random());
		if (initialDelay.isPresent() && repeatedCount == 0) {
			ticks = initialDelay.get();
		}
		game.scheduler().runAfterTicks(ticks, () -> {
			actions.apply(game, context, targets);
			schedule(game, context, targets, repeatedCount + 1);
		});
	}
}
