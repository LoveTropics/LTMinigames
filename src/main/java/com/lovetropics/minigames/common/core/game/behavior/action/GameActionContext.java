package com.lovetropics.minigames.common.core.game.behavior.action;

import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;

import java.util.Map;
import java.util.Optional;

public class GameActionContext {
	public static final GameActionContext EMPTY = new GameActionContext(Map.of());

	private final Map<GameActionParameter<?>, Object> parameters;

	private GameActionContext(Map<GameActionParameter<?>, Object> parameters) {
		this.parameters = parameters;
	}

	public static Builder builder() {
		return new Builder();
	}

	@SuppressWarnings("unchecked")
	public <T> Optional<T> get(GameActionParameter<T> parameter) {
		return Optional.ofNullable((T) parameters.get(parameter));
	}

	public static class Builder {
		private final Map<GameActionParameter<?>, Object> parameters = new Reference2ObjectArrayMap<>();

		private Builder() {
		}

		public <T> Builder set(GameActionParameter<T> parameter, T value) {
			parameters.put(parameter, value);
			return this;
		}

		public GameActionContext build() {
			return new GameActionContext(parameters);
		}
	}
}
