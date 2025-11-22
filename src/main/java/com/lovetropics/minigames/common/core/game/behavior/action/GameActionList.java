package com.lovetropics.minigames.common.core.game.behavior.action;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GameEventListeners;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.context.ContextMap;

import java.util.Optional;

public class GameActionList {
	public static final GameActionList EMPTY = new GameActionList(IGameBehavior.EMPTY, ActionTarget.PASS, 1);

	private static final int NO_MAX_RUNS = Integer.MAX_VALUE;

	public static final MapCodec<GameActionList> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			IGameBehavior.CODEC.fieldOf("actions").forGetter(list -> list.behavior),
			ActionTarget.CODEC.optionalFieldOf("target", ActionTarget.PASS).forGetter(list -> list.target),
			ExtraCodecs.POSITIVE_INT.optionalFieldOf("max_runs", NO_MAX_RUNS).forGetter(list -> list.maxRuns)
	).apply(i, GameActionList::new));

	private static final Codec<GameActionList> FULL_CODEC = MAP_CODEC.codec();
	private static final Codec<GameActionList> SIMPLE_CODEC = IGameBehavior.CODEC.xmap(
			behavior -> new GameActionList(behavior, ActionTarget.PASS, NO_MAX_RUNS),
			list -> list.behavior
	);

	// Use custom codec for better error reporting
	public static final Codec<GameActionList> CODEC = new Codec<>() {
		@Override
		public <T> DataResult<Pair<GameActionList, T>> decode(DynamicOps<T> ops, T input) {
			Optional<MapLike<T>> map = ops.getMap(input).result();
			if (map.isPresent() && map.get().get("actions") != null && map.get().get("type") == null) {
				return FULL_CODEC.decode(ops, input);
			}
			return SIMPLE_CODEC.decode(ops, input);
		}

		@Override
		public <T> DataResult<T> encode(GameActionList list, DynamicOps<T> ops, T prefix) {
			if (list.target.equals(ActionTarget.PASS) && list.maxRuns == NO_MAX_RUNS) {
				return SIMPLE_CODEC.encode(list, ops, prefix);
			}
			return FULL_CODEC.encode(list, ops, prefix);
		}
	};

	private final IGameBehavior behavior;
	private final ActionTarget target;
	private final int maxRuns;

	private final GameEventListeners listeners = new GameEventListeners();

	private boolean registered;
	private int runCount;

	public GameActionList(IGameBehavior behavior, ActionTarget target, int maxRuns) {
		this.behavior = behavior;
		this.target = target;
		this.maxRuns = maxRuns;
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

	public boolean apply(IGamePhase game, ContextMap context) {
		return apply(game, context, ActionSubjects.EMPTY);
	}

	public boolean apply(IGamePhase game, ContextMap context, ActionSubjects<?> sources) {
		if (isEmpty()) {
			return true;
		}
		if (!registered) {
			throw new IllegalStateException("Cannot dispatch action, GameActionList has not been registered");
		}
		if (++runCount > maxRuns) {
			return false;
		}
		ActionSubjects<?> targets = target.resolveTargets(game, sources);
		return listeners.invoker(GameActionEvents.APPLY).apply(context, targets);
	}

	private boolean isEmpty() {
		return behavior == IGameBehavior.EMPTY;
	}
}
