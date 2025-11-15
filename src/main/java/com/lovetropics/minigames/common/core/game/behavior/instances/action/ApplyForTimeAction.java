package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GameEventListeners;
import com.lovetropics.minigames.common.core.game.behavior.event.GameEventType;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.MutableInvoker;
import com.lovetropics.minigames.common.core.game.util.TemplatedText;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public record ApplyForTimeAction(
		GameActionList<ServerPlayer> apply,
		GameActionList<ServerPlayer> clear,
		GameActionList<ServerPlayer> tick,
		// Slightly sketchy implications for plugging any behavior in here, but oh well
		IGameBehavior nested,
		Optional<TemplatedText> indicator,
		int seconds
) implements IGameBehavior {
	public static final MapCodec<ApplyForTimeAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			GameActionList.PLAYER_CODEC.optionalFieldOf("apply", GameActionList.EMPTY_PLAYER).forGetter(ApplyForTimeAction::apply),
			GameActionList.PLAYER_CODEC.optionalFieldOf("clear", GameActionList.EMPTY_PLAYER).forGetter(ApplyForTimeAction::clear),
			GameActionList.PLAYER_CODEC.optionalFieldOf("tick", GameActionList.EMPTY_PLAYER).forGetter(ApplyForTimeAction::tick),
			IGameBehavior.CODEC.optionalFieldOf("nested", IGameBehavior.EMPTY).forGetter(ApplyForTimeAction::nested),
			TemplatedText.CODEC.optionalFieldOf("indicator").forGetter(ApplyForTimeAction::indicator),
			Codec.INT.fieldOf("seconds").forGetter(ApplyForTimeAction::seconds)
	).apply(i, ApplyForTimeAction::new));

	@Override
	public void register(final IGamePhase game, final EventRegistrar events) {
		apply.register(game, events);
		tick.register(game, events);
		clear.register(game, events);

		final State state = new State();
		nested.register(game, state.nestedListeners);
		for (final GameEventType<?> type : state.nestedListeners.eventTypes()) {
			state.nestedInvokers.put(type, MutableInvoker.addTo(events, type));
		}

		events.listen(GameActionEvents.APPLY, context -> state.tryApply(game, context));
		events.listen(GameActionEvents.APPLY_TO_PLAYER, (context, player) -> state.tryApplyTo(game, context, player));
		events.listen(GamePhaseEvents.TICK, () -> state.tick(game));
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.APPLY_FOR_TIME;
	}

	private class State {
		private static final long NOT_ACTIVE = -1;

		private final GameEventListeners nestedListeners = new GameEventListeners();
		private final Map<GameEventType<?>, MutableInvoker<?>> nestedInvokers = new HashMap<>();

		private long finishTime = NOT_ACTIVE;
		private final Object2LongMap<UUID> playerFinishTimes = new Object2LongOpenHashMap<>();

		private State() {
			playerFinishTimes.defaultReturnValue(NOT_ACTIVE);
		}

		private void tick(final IGamePhase game) {
			final long time = game.ticks();
			if (finishTime != NOT_ACTIVE) {
				tick.apply(game, ContextMap.EMPTY);
				if (time >= finishTime) {
					clear.apply(game, ContextMap.EMPTY);
					nestedInvokers.forEach((type, invoker) -> invoker.clear());
					finishTime = NOT_ACTIVE;
				}
			}
			playerFinishTimes.object2LongEntrySet().removeIf(entry -> tickPlayer(game, entry, time));
		}

		private boolean tickPlayer(IGamePhase game, Object2LongMap.Entry<UUID> entry, long time) {
			final ServerPlayer player = game.allPlayers().getPlayerBy(entry.getKey());
			if (player != null) {
				tick.apply(game, ContextMap.EMPTY, player);
			}

			final long finishTime = entry.getLongValue();
			if (time >= finishTime) {
				if (player != null) {
					clear.apply(game, ContextMap.EMPTY, player);
				}
				return true;
			} else {
				if (player != null && indicator.isPresent()) {
					tickIndicator(player, finishTime - time, indicator.get());
				}
				return false;
			}
		}

		private void tickIndicator(final ServerPlayer player, final long ticksLeft, final TemplatedText text) {
			if (ticksLeft % 5 == 0) {
				final int seconds = Mth.positiveCeilDiv((int) ticksLeft, SharedConstants.TICKS_PER_SECOND);
				player.sendSystemMessage(text.apply(Map.of("seconds", Component.literal(String.valueOf(seconds)))), true);
			}
		}

		private boolean tryApply(final IGamePhase game, final ContextMap context) {
			if (finishTime == NOT_ACTIVE && apply.apply(game, context)) {
				nestedInvokers.forEach((type, invoker) ->
						invoker.setUnchecked(nestedListeners.invoker(type))
				);
				finishTime = game.ticks() + (long) seconds * SharedConstants.TICKS_PER_SECOND;
				return true;
			}
			return false;
		}

		public boolean tryApplyTo(final IGamePhase game, final ContextMap context, final ServerPlayer player) {
			if (!playerFinishTimes.containsKey(player.getUUID()) && apply.apply(game, context, player)) {
				playerFinishTimes.put(player.getUUID(), game.ticks() + (long) seconds * SharedConstants.TICKS_PER_SECOND);
				return true;
			}
			return false;
		}
	}
}
