package com.lovetropics.minigames.common.core.game.behavior.action;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GameEventListeners;
import com.lovetropics.minigames.common.core.game.state.team.GameTeam;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.Entity;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class GameActionList<T> {
	public static final GameActionList<Entity> EMPTY_ENTITY = new GameActionList<>(IGameBehavior.EMPTY, EntityActionTarget.UNIT);
	public static final GameActionList<ServerPlayer> EMPTY_PLAYER = new GameActionList<>(IGameBehavior.EMPTY, PlayerActionTarget.SOURCE);
	public static final GameActionList<GameTeam> EMPTY_TEAM = new GameActionList<>(IGameBehavior.EMPTY, TeamActionTarget.SOURCE);
	public static final GameActionList<Void> EMPTY_VOID = new GameActionList<>(IGameBehavior.EMPTY, NoneActionTarget.INSTANCE);

	public static final MapCodec<GameActionList<?>> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			IGameBehavior.CODEC.fieldOf("actions").forGetter(list -> list.behavior),
			ActionTarget.FALLBACK_PLAYER.optionalFieldOf("target", PlayerActionTarget.SOURCE).forGetter(list -> list.target)
	).apply(i, GameActionList::new));

	public static final MapCodec<GameActionList<ServerPlayer>> PLAYER_MAP_CODEC = mapCodec(ActionTargetTypes.PLAYER, PlayerActionTarget.SOURCE);
	public static final MapCodec<GameActionList<Void>> VOID_MAP_CODEC = mapCodec(ActionTargetTypes.NONE, NoneActionTarget.INSTANCE);
	public static final Codec<GameActionList<Entity>> ENTITY_CODEC = codec(ActionTargetTypes.ENTITY, EntityActionTarget.UNIT);
	public static final Codec<GameActionList<ServerPlayer>> PLAYER_CODEC = codec(ActionTargetTypes.PLAYER, PlayerActionTarget.SOURCE);
	public static final Codec<GameActionList<GameTeam>> TEAM_CODEC = codec(ActionTargetTypes.TEAM, TeamActionTarget.SOURCE);
	public static final Codec<GameActionList<Void>> VOID_CODEC = codec(ActionTargetTypes.NONE, NoneActionTarget.INSTANCE);

	public static <T, A extends ActionTarget<T>> MapCodec<GameActionList<T>> mapCodec(Supplier<Codec<A>> type, A target) {
		return RecordCodecBuilder.mapCodec(i -> i.group(
				IGameBehavior.CODEC.fieldOf("actions").forGetter(list -> list.behavior),
				Codec.lazyInitialized(type).optionalFieldOf("target", target).forGetter(list -> (A) list.target)
		).apply(i, GameActionList::new));
	}

	public static <T, A extends ActionTarget<T>> Codec<GameActionList<T>> codec(Supplier<Codec<A>> type, A target) {
		var simpleCodec = IGameBehavior.CODEC.flatComapMap(
				behavior -> new GameActionList<>(behavior, target),
				list -> {
					if (!target.equals(list.target)) {
						return DataResult.error(() -> "Cannot encode simple action list with target: " + list.target);
					}
					return DataResult.success(list.behavior);
				}
		);

		Codec<GameActionList<T>> mapCodec = mapCodec(type, target).codec();

		// Use custom codec for better error reporting
		return new Codec<>() {
			@Override
			public <D> DataResult<Pair<GameActionList<T>, D>> decode(DynamicOps<D> ops, D input) {
				Optional<MapLike<D>> map = ops.getMap(input).result();
				if (map.isPresent() && map.get().get("actions") != null && map.get().get("type") == null) {
					return mapCodec.decode(ops, input);
				}
				return simpleCodec.decode(ops, input);
			}

			@Override
			public <D> DataResult<D> encode(GameActionList<T> input, DynamicOps<D> ops, D prefix) {
				return mapCodec.encode(input, ops, prefix);
			}
		};
	}

	private final IGameBehavior behavior;
	public final ActionTarget<T> target;

	private final GameEventListeners listeners = new GameEventListeners();

	private boolean registered;

	public GameActionList(IGameBehavior behavior, ActionTarget<T> target) {
		this.behavior = behavior;
		this.target = target;
	}

	public void register(IGamePhase game, EventRegistrar events) {
		if (isEmpty()) {
			return;
		}
		if (registered) {
			throw new IllegalStateException("GameActionList has already been registered");
		}
		behavior.register(game, events.redirect(GameActionEvents::matches, listeners));
		registered = true;
	}

	public <T1> boolean applyIf(Codec<? extends ActionTarget<T1>> type, IGamePhase phase, ContextMap context, Iterable<T1> sources) {
		if (type == target.type()) {
			return apply(phase, context, (Iterable<T>) sources);
		}
		return false;
	}

	public <T1> boolean applyIf(Supplier<? extends Codec<? extends ActionTarget<T1>>> type, IGamePhase phase, ContextMap context, Iterable<T1> sources) {
		return applyIf(type.get(), phase, context, sources);
	}

	public boolean apply(IGamePhase phase, ContextMap context) {
		return apply(phase, context, target.resolve(phase, List.of()));
	}

	public boolean apply(IGamePhase phase, ContextMap context, T... sources) {
		return apply(phase, context, Arrays.asList(sources));
	}

	public boolean apply(IGamePhase phase, ContextMap context, Iterable<T> sources) {
		if (isEmpty()) {
			return true;
		}
		if (!registered) {
			throw new IllegalStateException("Cannot dispatch action, GameActionList has not been registered");
		}
		return listeners.invoker(GameActionEvents.APPLY).apply(context) | target.apply(phase, listeners, context, sources);
	}

	private boolean isEmpty() {
		return behavior == IGameBehavior.EMPTY;
	}
}
