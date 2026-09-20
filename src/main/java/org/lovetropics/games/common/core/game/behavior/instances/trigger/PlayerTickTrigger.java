package org.lovetropics.games.common.core.game.behavior.instances.trigger;

import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorType;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorTypes;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.ActionSubjects;
import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.util.context.ContextMap;

import java.util.Optional;
import java.util.function.Supplier;

public record PlayerTickTrigger(Optional<EntityPredicate> predicate, GameActionList action) implements IGameBehavior {
	public static final MapCodec<PlayerTickTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			EntityPredicate.CODEC.optionalFieldOf("predicate").forGetter(PlayerTickTrigger::predicate),
			GameActionList.MAP_CODEC.forGetter(PlayerTickTrigger::action)
	).apply(i, PlayerTickTrigger::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		action.register(game, events);

		events.listen(GamePlayerEvents.TICK, player -> {
			if (predicate.isEmpty() || predicate.get().matches(player, player)) {
				action.apply(game, ContextMap.EMPTY, ActionSubjects.ofPlayer(player));
			}
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.PLAYER_TICK;
	}
}
