package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;

public record DelayedAction(
		IntProvider delay,
		GameActionList actions
) implements IGameBehavior {
	public static final MapCodec<DelayedAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			IntProvider.POSITIVE_CODEC.fieldOf("delay").forGetter(b -> b.delay),
			GameActionList.MAP_CODEC.forGetter(c -> c.actions)
	).apply(i, DelayedAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		actions.register(game, events);

		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			int ticks = delay.sample(game.random());
			game.scheduler().runAfterTicks(ticks, () ->
					actions.apply(game, context, targets)
			);
			return true;
		});
	}
}
