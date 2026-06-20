package com.lovetropics.minigames.common.core.game.behavior.instances.trigger;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionSubjects;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionContextKeys;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.util.Util;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.util.context.ContextMap;

import java.util.Optional;
import java.util.function.Supplier;

public record OnDeathTrigger(GameActionList killedAction, GameActionList killerAction, Optional<EntityPredicate> killedPredicate, Optional<EntityPredicate> killerPredicate, boolean excludeSelf) implements IGameBehavior {
	public static final MapCodec<OnDeathTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			GameActionList.CODEC.optionalFieldOf("killed_action", GameActionList.EMPTY).forGetter(OnDeathTrigger::killedAction),
			GameActionList.CODEC.optionalFieldOf("killer_action", GameActionList.EMPTY).forGetter(OnDeathTrigger::killerAction),
			EntityPredicate.CODEC.optionalFieldOf("killed_predicate").forGetter(OnDeathTrigger::killedPredicate),
			EntityPredicate.CODEC.optionalFieldOf("killer_predicate").forGetter(OnDeathTrigger::killerPredicate),
			Codec.BOOL.optionalFieldOf("exclude_self", false).forGetter(OnDeathTrigger::excludeSelf)
	).apply(i, OnDeathTrigger::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		killedAction.register(game, events);
		killerAction.register(game, events);

		events.listen(GamePlayerEvents.DEATH, (player, damageSource) -> {
			final ServerPlayer killer = Util.getKillerPlayer(player, damageSource);
			if (excludeSelf && killer == player) {
				return TriState.DEFAULT;
			}
			if (killerPredicate.isPresent() && !killerPredicate.get().matches(player, killer)) {
				return TriState.DEFAULT;
			}
			if (killedPredicate.isPresent() && !killedPredicate.get().matches(player, player)) {
				return TriState.DEFAULT;
			}
			final ContextMap.Builder context = new ContextMap.Builder()
					.withParameter(GameActionContextKeys.KILLED, player);
			if (killer != null) {
				killedAction.apply(game, context.withParameter(GameActionContextKeys.KILLER, killer).create(ContextKeySet.EMPTY), ActionSubjects.ofPlayer(player));
				killerAction.apply(game, context.withParameter(GameActionContextKeys.KILLER, killer).create(ContextKeySet.EMPTY), ActionSubjects.ofPlayer(killer));
			} else {
				killedAction.apply(game, context.create(ContextKeySet.EMPTY), ActionSubjects.ofPlayer(player));
			}
			return TriState.DEFAULT;
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.ON_DEATH;
	}
}
