package org.lovetropics.games.common.core.game.behavior.instances.trigger;

import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorType;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorTypes;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.ActionSubjects;
import org.lovetropics.games.common.core.game.behavior.action.ActionTarget;
import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.state.progress.ProgressChannel;
import org.lovetropics.games.common.core.game.state.progress.ProgressionPoint;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.context.ContextMap;

import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public record ScheduledActionsTrigger(ActionTarget target, ProgressChannel channel, Map<ProgressionPoint, GameActionList> scheduledActions) implements IGameBehavior {
	public static final MapCodec<ScheduledActionsTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ActionTarget.CODEC.optionalFieldOf("target", ActionTarget.PASS).forGetter(t -> t.target),
			ProgressChannel.CODEC.optionalFieldOf("channel", ProgressChannel.MAIN).forGetter(ScheduledActionsTrigger::channel),
			Codec.unboundedMap(ProgressionPoint.STRING_CODEC, GameActionList.CODEC).fieldOf("actions").forGetter(ScheduledActionsTrigger::scheduledActions)
	).apply(i, ScheduledActionsTrigger::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		for (GameActionList actions : scheduledActions.values()) {
			actions.register(game, events);
		}

		List<Pair<BooleanSupplier, GameActionList>> actions = scheduledActions.entrySet().stream()
				.map(entry -> Pair.of(entry.getKey().createPredicate(game, channel), entry.getValue()))
				.collect(Collectors.toList());

		events.listen(GamePhaseEvents.TICK, () -> actions.removeIf(entry -> {
			if (entry.getFirst().getAsBoolean()) {
				entry.getSecond().apply(game, ContextMap.EMPTY, target.resolveTargets(game, ActionSubjects.EMPTY));
				return true;
			}
			return false;
		}));
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.SCHEDULED_ACTIONS;
	}
}
