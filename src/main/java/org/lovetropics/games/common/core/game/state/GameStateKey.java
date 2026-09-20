package org.lovetropics.games.common.core.game.state;

import java.util.function.Supplier;

public class GameStateKey<T extends IGameState> {
	private final String name;

	private GameStateKey(String name) {
		this.name = name;
	}

	public static <T extends IGameState> GameStateKey<T> create(String name) {
		return new GameStateKey<>(name);
	}

	public static <T extends IGameState> GameStateKey.Defaulted<T> create(String name, Supplier<T> defaultFactory) {
		return new GameStateKey.Defaulted<>(name, defaultFactory);
	}

	public String getName() {
		return name;
	}

	public static final class Defaulted<T extends IGameState> extends GameStateKey<T> {
		private final Supplier<T> defaultFactory;

		Defaulted(String name, Supplier<T> defaultFactory) {
			super(name);
			this.defaultFactory = defaultFactory;
		}

		public T createDefault() {
			return defaultFactory.get();
		}
	}
}
