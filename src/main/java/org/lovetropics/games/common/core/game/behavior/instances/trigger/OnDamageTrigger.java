package org.lovetropics.games.common.core.game.behavior.instances.trigger;

import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.ActionSubjects;
import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.util.TriState;
import net.minecraft.util.context.ContextMap;

import java.util.Optional;

public record OnDamageTrigger(GameActionList actions, Optional<EntityPredicate> predicate) implements IGameBehavior {

	public static final MapCodec<OnDamageTrigger> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			GameActionList.MAP_CODEC.forGetter(OnDamageTrigger::actions),
			EntityPredicate.CODEC.optionalFieldOf("predicate").forGetter(OnDamageTrigger::predicate)
	).apply(instance, OnDamageTrigger::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		actions.register(game, events);

		events.listen(GamePlayerEvents.DAMAGE, (player, damageSource, amount) -> {
			if (predicate.map(predicate -> predicate.matches(player, player)).orElse(true)) {
				actions.apply(game, ContextMap.EMPTY, ActionSubjects.ofPlayer(player));
			}
			return TriState.DEFAULT;
		});
	}
}
