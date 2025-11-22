package com.lovetropics.minigames.common.core.game.behavior.instances.trigger;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionSubjects;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionTarget;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.util.context.ContextMap;

import java.util.Collection;
import java.util.function.Supplier;

public record DelayedSequenceAction(ActionTarget target, Long2ObjectMap<GameActionList> actions) implements IGameBehavior {
	public static final MapCodec<DelayedSequenceAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ActionTarget.CODEC.optionalFieldOf("target", ActionTarget.PASS).forGetter(DelayedSequenceAction::target),
			MoreCodecs.long2Object(GameActionList.CODEC).fieldOf("actions").forGetter(DelayedSequenceAction::actions)
	).apply(i, DelayedSequenceAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		for (GameActionList actions : actions.values()) {
			actions.register(game, events);
		}

		Multimap<Long, ScheduledAction> scheduled = HashMultimap.create();
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			ActionSubjects<?> modifiedTargets = target.resolveTargets(game, targets);
			for (Long2ObjectMap.Entry<GameActionList> entry : actions.long2ObjectEntrySet()) {
				scheduled.put(game.ticks() + entry.getLongKey(), new ScheduledAction(entry.getValue(), context, modifiedTargets));
			}
			return true;
		});

		events.listen(GamePhaseEvents.TICK, () -> {
			Collection<ScheduledAction> actions = scheduled.removeAll(game.ticks());
			for (ScheduledAction action : actions) {
				action.actions.apply(game, action.context, action.subjects);
			}
		});
	}

	private record ScheduledAction(GameActionList actions, ContextMap context, ActionSubjects<?> subjects) {
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.DELAYED_SEQUENCE;
	}
}
