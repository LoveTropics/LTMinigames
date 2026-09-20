package org.lovetropics.games.common.core.game.state.progress;

import org.lovetropics.games.common.core.game.state.GameStateKey;
import org.lovetropics.games.common.core.game.state.IGameState;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Map;

public final class GameProgressionState implements IGameState {
	public static final GameStateKey.Defaulted<GameProgressionState> KEY = GameStateKey.create("Game Progression", GameProgressionState::new);

	private final Map<ProgressChannel, ProgressHolder> holders = new Object2ObjectOpenHashMap<>();

	public ProgressHolder register(ProgressChannel channel) {
		ProgressHolder holder = new ProgressHolder();
		if (holders.putIfAbsent(channel, holder) != null) {
			throw new IllegalStateException("Cannot register progression for channel: " + channel + ", as it is already registered");
		}
		return holder;
	}

	public ProgressHolder getOrThrow(ProgressChannel channel) {
		ProgressHolder holder = holders.get(channel);
		if (holder == null) {
			throw new IllegalArgumentException("No progression registered for channel: " + channel);
		}
		return holder;
	}
}
