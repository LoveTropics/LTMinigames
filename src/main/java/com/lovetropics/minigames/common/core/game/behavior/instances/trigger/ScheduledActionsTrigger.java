package com.lovetropics.minigames.common.core.game.behavior.instances.trigger;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.action.PlayerActionTarget;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressChannel;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressionPoint;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.context.ContextMap;

import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;

// TODO ideally this would use a void target.. but we have too many uses using a player
public record ScheduledActionsTrigger(PlayerActionTarget target, ProgressChannel channel, Map<ProgressionPoint, GameActionList<ServerPlayer>> scheduledActions) implements IGameBehavior {
	public static final MapCodec<ScheduledActionsTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			PlayerActionTarget.Target.CODEC.xmap(PlayerActionTarget::new, PlayerActionTarget::target).optionalFieldOf("target", PlayerActionTarget.SOURCE).forGetter(ScheduledActionsTrigger::target),
			ProgressChannel.CODEC.optionalFieldOf("channel", ProgressChannel.MAIN).forGetter(ScheduledActionsTrigger::channel),
			Codec.unboundedMap(ProgressionPoint.STRING_CODEC, GameActionList.PLAYER_CODEC).fieldOf("actions").forGetter(ScheduledActionsTrigger::scheduledActions)
	).apply(i, ScheduledActionsTrigger::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		for (GameActionList<ServerPlayer> actions : scheduledActions.values()) {
			actions.register(game, events);
		}

		final List<Pair<BooleanSupplier, GameActionList<ServerPlayer>>> actions = scheduledActions.entrySet().stream()
				.map(entry -> Pair.of(entry.getKey().createPredicate(game, channel), entry.getValue()))
				.collect(Collectors.toList());

		events.listen(GamePhaseEvents.TICK, () -> actions.removeIf(entry -> {
			if (entry.getFirst().getAsBoolean()) {
				entry.getSecond().apply(game, ContextMap.EMPTY, target.resolve(game, List.of()));
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
