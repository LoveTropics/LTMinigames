package com.lovetropics.minigames.common.core.game.behavior.instances.trigger;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionContext;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionParameter;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.util.Util;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;

import java.util.Optional;
import java.util.function.Supplier;

public record OnDeathTrigger(GameActionList<ServerPlayer> killedAction, GameActionList<ServerPlayer> killerAction, Optional<EntityPredicate> killedPredicate, Optional<EntityPredicate> killerPredicate, boolean excludeSelf) implements IGameBehavior {
	public static final MapCodec<OnDeathTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			GameActionList.PLAYER_CODEC.optionalFieldOf("killed_action", GameActionList.EMPTY).forGetter(OnDeathTrigger::killedAction),
			GameActionList.PLAYER_CODEC.optionalFieldOf("killer_action", GameActionList.EMPTY).forGetter(OnDeathTrigger::killerAction),
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
			final GameActionContext.Builder context = GameActionContext.builder().set(GameActionParameter.KILLED, player);
			if (killer != null) {
				killedAction.apply(game, context.set(GameActionParameter.KILLER, killer).build(), player);
				killerAction.apply(game, context.set(GameActionParameter.KILLER, killer).build(), killer);
			} else {
				killedAction.apply(game, context.build(), player);
			}
			return TriState.DEFAULT;
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.ON_DEATH;
	}
}
