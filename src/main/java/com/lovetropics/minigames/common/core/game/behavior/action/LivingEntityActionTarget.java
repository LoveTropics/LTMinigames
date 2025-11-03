package com.lovetropics.minigames.common.core.game.behavior.action;

import com.google.common.collect.Lists;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GameEventListeners;
import com.mojang.serialization.Codec;
import net.minecraft.world.entity.LivingEntity;
import org.apache.commons.lang3.function.ToBooleanBiFunction;

import java.util.List;

public record LivingEntityActionTarget() implements ActionTarget<LivingEntity> {
	public static final LivingEntityActionTarget UNIT = new LivingEntityActionTarget();

	public static final Codec<LivingEntityActionTarget> CODEC = Codec.unit(UNIT);

	@Override
	public List<LivingEntity> resolve(IGamePhase phase, Iterable<LivingEntity> sources) {
		return Lists.newArrayList(sources);
	}

	@Override
	public boolean apply(IGamePhase game, GameEventListeners listeners, GameActionContext actionContext, Iterable<LivingEntity> sources) {
		boolean result = false;
		for (var target : Lists.newArrayList(sources)) {
			result |= listeners.invoker(GameActionEvents.APPLY_TO_ENTITY).apply(actionContext, target);
			result |= listeners.invoker(GameActionEvents.APPLY_TO_LIVING_ENTITY).apply(actionContext, target);
		}
		return result;
	}

	@Override
	public void listenAndCaptureSource(EventRegistrar listeners, ToBooleanBiFunction<GameActionContext, Iterable<LivingEntity>> listener) {
		listeners.listen(GameActionEvents.APPLY_TO_LIVING_ENTITY, (context, target) -> listener.applyAsBoolean(context, List.of(target)));
	}

	@Override
	public Codec<LivingEntityActionTarget> type() {
		return ActionTargetTypes.LIVING_ENTITY.get();
	}
}
