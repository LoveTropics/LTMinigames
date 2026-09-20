package org.lovetropics.games.common.core.game.behavior.instances.action;

import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorType;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorTypes;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.ActionSubjects;
import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.core.game.behavior.event.GameEventListeners;
import org.lovetropics.games.common.core.game.behavior.event.GameEventType;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.event.MutableInvoker;
import org.lovetropics.games.common.core.game.state.ActionMutex;
import org.lovetropics.games.common.core.game.state.ActionMutexState;
import org.lovetropics.games.common.core.game.util.TemplatedText;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextMap;

import org.jspecify.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public record ApplyForTimeAction(
		GameActionList apply,
		GameActionList clear,
		GameActionList tick,
		// Slightly sketchy implications for plugging any behavior in here, but oh well
		IGameBehavior nested,
		Optional<TemplatedText> indicator,
		int seconds,
		Optional<Identifier> mutex,
		boolean forceAcquireMutex,
		boolean splitByPlayer
) implements IGameBehavior {
	public static final MapCodec<ApplyForTimeAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			GameActionList.CODEC.optionalFieldOf("apply", GameActionList.EMPTY).forGetter(ApplyForTimeAction::apply),
			GameActionList.CODEC.optionalFieldOf("clear", GameActionList.EMPTY).forGetter(ApplyForTimeAction::clear),
			GameActionList.CODEC.optionalFieldOf("tick", GameActionList.EMPTY).forGetter(ApplyForTimeAction::tick),
			IGameBehavior.CODEC.optionalFieldOf("nested", IGameBehavior.EMPTY).forGetter(ApplyForTimeAction::nested),
			TemplatedText.CODEC.optionalFieldOf("indicator").forGetter(ApplyForTimeAction::indicator),
			Codec.INT.fieldOf("seconds").forGetter(ApplyForTimeAction::seconds),
			Identifier.CODEC.optionalFieldOf("mutex").forGetter(ApplyForTimeAction::mutex),
			Codec.BOOL.optionalFieldOf("force_acquire_mutex", false).forGetter(ApplyForTimeAction::forceAcquireMutex),
			// TODO: This feels easy to mess up
			Codec.BOOL.optionalFieldOf("split_by_player", false).forGetter(ApplyForTimeAction::splitByPlayer)
	).apply(i, ApplyForTimeAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		apply.register(game, events);
		tick.register(game, events);
		clear.register(game, events);

		State state = new State();
		nested.register(game, state.nestedListeners);
		for (GameEventType<?> type : state.nestedListeners.eventTypes()) {
			state.nestedInvokers.put(type, MutableInvoker.addTo(events, type));
		}

		events.listen(GameActionEvents.APPLY, (context, targets) -> state.tryApply(game, context, targets));
		events.listen(GamePhaseEvents.TICK, () -> state.tick(game));
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.APPLY_FOR_TIME;
	}

	private class State {
		private final GameEventListeners nestedListeners = new GameEventListeners();
		private final Map<GameEventType<?>, MutableInvoker<?>> nestedInvokers = new HashMap<>();

		private @Nullable ActiveAction globalAction;
		private final Map<UUID, ActiveAction> playerActions = new HashMap<>();

		private void tick(IGamePhase game) {
			long time = game.ticks();
			if (globalAction != null && tickGlobalAction(game, globalAction, time)) {
				clearNestedInvokers();
				globalAction = null;
			}
			playerActions.entrySet().removeIf(entry ->
					tickPlayer(game, entry.getKey(), entry.getValue(), time)
			);
		}

		private boolean tickGlobalAction(IGamePhase game, ActiveAction action, long time) {
			if (!action.isMutexValid()) {
				return true;
			}
			tick.apply(game, ContextMap.EMPTY);
			if (time >= action.finishTime) {
				clear.apply(game, ContextMap.EMPTY);
				action.releaseMutex();
				return true;
			}
			return false;
		}

		private void clearNestedInvokers() {
			nestedInvokers.forEach((type, invoker) -> invoker.clear());
		}

		private boolean tickPlayer(IGamePhase game, UUID playerId, ActiveAction action, long time) {
			if (!action.isMutexValid()) {
				return true;
			}

			ServerPlayer player = game.allPlayers().getPlayerBy(playerId);
			if (player != null) {
				tick.apply(game, ContextMap.EMPTY, ActionSubjects.ofPlayer(player));
			}

			if (time >= action.finishTime) {
				if (player != null) {
					clear.apply(game, ContextMap.EMPTY, ActionSubjects.ofPlayer(player));
				}
				action.releaseMutex();
				return true;
			} else {
				if (player != null && indicator.isPresent()) {
					tickIndicator(player, action.finishTime - time, indicator.get());
				}
				return false;
			}
		}

		private void tickIndicator(ServerPlayer player, long ticksLeft, TemplatedText text) {
			if (ticksLeft % 5 == 0) {
				int seconds = Mth.positiveCeilDiv((int) ticksLeft, SharedConstants.TICKS_PER_SECOND);
				player.sendSystemMessage(text.apply(Map.of("seconds", Component.literal(String.valueOf(seconds)))), true);
			}
		}

		private boolean tryApply(IGamePhase game, ContextMap context, ActionSubjects<?> targets) {
			long newFinishTime = game.ticks() + (long) seconds * SharedConstants.TICKS_PER_SECOND;

			ActionMutexState mutexes = game.state().get(ActionMutexState.KEY);
			if (splitByPlayer) {
				boolean applied = false;
				for (ServerPlayer player : targets.asPlayers(game)) {
					applied |= tryApplyForPlayer(game, context, player, mutexes, newFinishTime);
				}
				return applied;
			} else {
				return tryApplyGlobal(game, mutexes, context, newFinishTime);
			}
		}

		private boolean tryApplyGlobal(IGamePhase game, ActionMutexState mutexes, ContextMap context, long newFinishTime) {
			if (globalAction != null) {
				return false;
			}
			ActionMutex acquiredMutex = null;
			if (mutex.isPresent()) {
				acquiredMutex = mutexes.acquireGlobal(mutex.get(), forceAcquireMutex);
				if (acquiredMutex == null) {
					return false;
				}
			}
			if (apply.apply(game, context)) {
				nestedInvokers.forEach((type, invoker) ->
						invoker.setUnchecked(nestedListeners.invoker(type))
				);
				globalAction = new ActiveAction(newFinishTime, acquiredMutex);
				return true;
			} else if (acquiredMutex != null) {
				acquiredMutex.close();
			}
			return false;
		}

		private boolean tryApplyForPlayer(IGamePhase game, ContextMap context, ServerPlayer player, ActionMutexState mutexes, long newFinishTime) {
			if (playerActions.containsKey(player.getUUID())) {
				return false;
			}
			ActionMutex acquiredMutex = null;
			if (mutex.isPresent()) {
				acquiredMutex = mutexes.acquireForPlayer(player, mutex.get(), forceAcquireMutex);
				if (acquiredMutex == null) {
					return false;
				}
			}
			if (apply.apply(game, context, ActionSubjects.ofPlayer(player))) {
				playerActions.put(player.getUUID(), new ActiveAction(newFinishTime, acquiredMutex));
				return true;
			} else if (acquiredMutex != null) {
				acquiredMutex.close();
			}
			return false;
		}

		private record ActiveAction(
				long finishTime,
				@Nullable ActionMutex mutex
		) {
			public boolean isMutexValid() {
				return mutex == null || mutex.isValid();
			}

			public void releaseMutex() {
				if (mutex != null) {
					mutex.close();
				}
			}
		}
	}
}
