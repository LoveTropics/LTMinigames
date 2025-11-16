package com.lovetropics.minigames.common.core.game.behavior.instances.trigger;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionSubjects;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionContextKeys;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

import java.util.Optional;
import java.util.function.Supplier;

public record OnEntityInteractionTrigger(
		GameActionList sourceActions,
		GameActionList targetActions,
		Optional<EntityPredicate> sourcePredicate,
		Optional<EntityPredicate> targetPredicate
) implements IGameBehavior {
	public static final MapCodec<OnEntityInteractionTrigger> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			GameActionList.CODEC.optionalFieldOf("source_actions", GameActionList.EMPTY).forGetter(OnEntityInteractionTrigger::sourceActions),
			GameActionList.CODEC.optionalFieldOf("target_actions", GameActionList.EMPTY).forGetter(OnEntityInteractionTrigger::targetActions),
			EntityPredicate.CODEC.optionalFieldOf("source_predicate").forGetter(OnEntityInteractionTrigger::sourcePredicate),
			EntityPredicate.CODEC.optionalFieldOf("target_predicate").forGetter(OnEntityInteractionTrigger::targetPredicate)
	).apply(instance, OnEntityInteractionTrigger::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		sourceActions.register(game, events);
		targetActions.register(game, events);

		events.listen(GamePlayerEvents.INTERACT_ENTITY, (player, target, hand) -> {
			if (hand != InteractionHand.MAIN_HAND) {
				return InteractionResult.PASS;
			}

			if (sourcePredicate.isPresent() && !sourcePredicate.get().matches(player, player)) {
				return InteractionResult.PASS;
			}
			if (targetPredicate.isPresent() && !targetPredicate.get().matches(player, target)) {
				return InteractionResult.PASS;
			}

			final ContextMap.Builder context = new ContextMap.Builder()
					.withParameter(GameActionContextKeys.TARGET, target);
			sourceActions.apply(game, context.create(ContextKeySet.EMPTY), ActionSubjects.ofPlayer(player));
			targetActions.apply(game, context.create(ContextKeySet.EMPTY), ActionSubjects.ofEntity(target));

			return InteractionResult.CONSUME;
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.ON_ENTITY_INTERACTION;
	}
}
