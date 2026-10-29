package org.lovetropics.games.common.core.game.behavior.instances.trigger;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.util.context.ContextMap;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.ActionSubjects;
import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;

import java.util.Optional;

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
	public MapCodec<? extends IGameBehavior> codec() {
		return CODEC;
	}
}
