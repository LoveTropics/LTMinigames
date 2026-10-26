package org.lovetropics.games.common.core.game.state;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lovetropics.games.common.core.game.GameException;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class GameStateMap {
	private final ConcurrentMap<GameStateKey<?>, IGameState> state = new ConcurrentHashMap<>();

	@SuppressWarnings("unchecked")
	public <T extends IGameState> T getOrRegister(GameStateKey<T> key, T state) {
		T oldState = (T) this.state.putIfAbsent(key, state);
		return oldState != null ? oldState : state;
	}

	public <T extends IGameState> T register(GameStateKey<T> key, T state) {
		if (this.state.putIfAbsent(key, state) == null) {
			return state;
		} else {
			throw new IllegalStateException("Multiple callers tried to register game state of key: " + key.getName());
		}
	}

	@SuppressWarnings("unchecked")
	public <T extends IGameState> T get(GameStateKey.Defaulted<T> key) {
		return (T) state.computeIfAbsent(key, k -> key.createDefault());
	}

	@SuppressWarnings("unchecked")
	public <T extends IGameState> @Nullable T getOrNull(GameStateKey<T> key) {
		return (T) state.get(key);
	}

	public <T extends IGameState> Optional<T> getOptional(GameStateKey<T> key) {
		return Optional.ofNullable(getOrNull(key));
	}

	public <T extends IGameState> T getOrThrow(GameStateKey<T> key) {
		T state = getOrNull(key);
		if (state == null) {
			throw new GameException(Component.literal("Missing expected game state of key: " + key.getName()));
		}
		return state;
	}

	public <T extends IGameState> T getOrDefault(GameStateKey<T> key, T orDefault) {
		return Objects.requireNonNullElse(getOrNull(key), orDefault);
	}
}
