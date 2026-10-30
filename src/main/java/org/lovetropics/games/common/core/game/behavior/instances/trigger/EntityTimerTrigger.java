package org.lovetropics.games.common.core.game.behavior.instances.trigger;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.Entity;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.ActionSubjects;
import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;

import java.util.List;
import java.util.Optional;

public record EntityTimerTrigger(
		Optional<EntityPredicate> whenMatching,
		Optional<EntityPredicate> whenNotMatching,
		int afterTicks,
		GameActionList actions
) implements IGameBehavior {
	public static final MapCodec<EntityTimerTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			EntityPredicate.CODEC.optionalFieldOf("when_matching").forGetter(EntityTimerTrigger::whenMatching),
			EntityPredicate.CODEC.optionalFieldOf("when_not_matching").forGetter(EntityTimerTrigger::whenMatching),
			Codec.INT.optionalFieldOf("after_ticks", 1).forGetter(EntityTimerTrigger::afterTicks),
			GameActionList.MAP_CODEC.forGetter(EntityTimerTrigger::actions)
	).apply(i, EntityTimerTrigger::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		actions.register(game, events);

		Reference2IntOpenHashMap<Entity> ticksByEntity = new Reference2IntOpenHashMap<>();
		events.listen(GamePhaseEvents.TICK, () -> {
			for (ServerLevel level : game.allLevels()) {
				for (Entity entity : level.getEntities().getAll()) {
					if (matches(level, entity)) {
						ticksByEntity.addTo(entity, 1);
					} else {
						ticksByEntity.removeInt(entity);
					}
				}
			}
			ObjectIterator<Reference2IntMap.Entry<Entity>> iterator = ticksByEntity.reference2IntEntrySet().iterator();
			while (iterator.hasNext()) {
				Reference2IntMap.Entry<Entity> entry = iterator.next();
				if (entry.getKey().isRemoved()) {
					iterator.remove();
					continue;
				}
				if (entry.getIntValue() > afterTicks) {
					actions.apply(game, ContextMap.EMPTY, ActionSubjects.ofEntities(List.of(entry.getKey())));
					iterator.remove();
				}
			}
		});
	}

	private boolean matches(ServerLevel level, Entity entity) {
		return (whenMatching.isEmpty() || whenMatching.get().matches(level, entity.position(), entity))
				&& (whenNotMatching.isEmpty() || !whenNotMatching.get().matches(level, entity.position(), entity));
	}
}
