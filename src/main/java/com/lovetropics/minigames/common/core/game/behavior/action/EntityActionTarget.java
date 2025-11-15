package com.lovetropics.minigames.common.core.game.behavior.action;

import com.google.common.collect.Lists;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GameEventListeners;
import com.mojang.serialization.Codec;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.Entity;
import org.apache.commons.lang3.function.ToBooleanBiFunction;

import java.util.List;

public record EntityActionTarget() implements ActionTarget<Entity> {
	public static final EntityActionTarget UNIT = new EntityActionTarget();

	public static final Codec<EntityActionTarget> CODEC = Codec.unit(UNIT);

	@Override
	public List<Entity> resolve(IGamePhase phase, Iterable<Entity> sources) {
		return Lists.newArrayList(sources);
	}

	@Override
	public boolean apply(IGamePhase game, GameEventListeners listeners, ContextMap actionContext, Iterable<Entity> sources) {
		boolean result = false;
		for (Entity target : Lists.newArrayList(sources)) {
			result |= listeners.invoker(GameActionEvents.APPLY_TO_ENTITY).apply(actionContext, target);
		}
		return result;
	}

	@Override
	public void listenAndCaptureSource(EventRegistrar listeners, ToBooleanBiFunction<ContextMap, Iterable<Entity>> listener) {
		listeners.listen(GameActionEvents.APPLY_TO_ENTITY, (context, target) -> listener.applyAsBoolean(context, List.of(target)));
	}

	@Override
	public Codec<EntityActionTarget> type() {
		return ActionTargetTypes.ENTITY.get();
	}
}
