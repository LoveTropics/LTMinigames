package com.lovetropics.minigames.common.core.game.behavior.instances.trigger;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.context.ContextMap;

import java.util.Optional;
import java.util.function.Supplier;

public record PlayerTickTrigger(Optional<EntityPredicate> predicate, GameActionList<ServerPlayer> action) implements IGameBehavior {
	public static final MapCodec<PlayerTickTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			EntityPredicate.CODEC.optionalFieldOf("predicate").forGetter(PlayerTickTrigger::predicate),
			GameActionList.PLAYER_CODEC.fieldOf("action").forGetter(PlayerTickTrigger::action)
	).apply(i, PlayerTickTrigger::new));

	@Override
	public void register(final IGamePhase game, final EventRegistrar events) {
		action.register(game, events);

		events.listen(GamePlayerEvents.TICK, player -> {
			if (predicate.isEmpty() || predicate.get().matches(player, player)) {
				action.apply(game, ContextMap.EMPTY, player);
			}
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.PLAYER_TICK;
	}
}
