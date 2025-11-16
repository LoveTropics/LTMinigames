package com.lovetropics.minigames.common.core.game.behavior.event;

import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.Entity;

import java.util.function.Function;
import java.util.function.Predicate;

public interface EventRegistrar {
	<T> void listen(GameEventType<T> type, T listener);

	<T> void unlisten(GameEventType<T> type, T listener);

	default void addAll(GameEventListeners listeners) {
		listeners.forEach(this::listen);
	}

	default void removeAll(GameEventListeners listeners) {
		listeners.forEach(this::unlisten);
	}

	default void applyToPlayers(IGamePhase game, ActionHandler<ServerPlayer> handler) {
		listen(GameActionEvents.APPLY, (context, targets) -> {
			boolean applied = false;
			for (ServerPlayer player : targets.asPlayers(game)) {
				applied |= handler.handle(context, player);
			}
			return applied;
		});
	}

	default void applyToEntities(IGamePhase game, ActionHandler<Entity> handler) {
		listen(GameActionEvents.APPLY, (context, targets) -> {
			boolean applied = false;
			for (Entity entity : targets.asEntities(game)) {
				applied |= handler.handle(context, entity);
			}
			return applied;
		});
	}

	default void applyToPlots(IGamePhase game, ActionHandler<Plot> handler) {
		listen(GameActionEvents.APPLY, (context, targets) -> {
			boolean applied = false;
			for (Plot plot : targets.asPlots(game)) {
				applied |= handler.handle(context, plot);
			}
			return applied;
		});
	}

	default void applyToTeams(IGamePhase game, ActionHandler<GameTeamKey> handler) {
		listen(GameActionEvents.APPLY, (context, targets) -> {
			boolean applied = false;
			for (GameTeamKey team : targets.asTeams(game)) {
				applied |= handler.handle(context, team);
			}
			return applied;
		});
	}

	default EventRegistrar redirect(Predicate<GameEventType<?>> predicate, EventRegistrar redirect) {
		return mapping(type -> predicate.test(type) ? redirect : this);
	}

	static EventRegistrar mapping(Function<GameEventType<?>, EventRegistrar> redirect) {
		return new EventRegistrar() {
			@Override
			public <T> void listen(GameEventType<T> type, T listener) {
				redirect.apply(type).listen(type, listener);
			}

			@Override
			public <T> void unlisten(GameEventType<T> type, T listener) {
				redirect.apply(type).unlisten(type, listener);
			}
		};
	}
	
	interface ActionHandler<T> {
		boolean handle(ContextMap context, T target);
	}
}
