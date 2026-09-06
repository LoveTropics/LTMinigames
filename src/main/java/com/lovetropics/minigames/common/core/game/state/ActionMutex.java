package com.lovetropics.minigames.common.core.game.state;

import org.jspecify.annotations.Nullable;
import java.util.UUID;

public class ActionMutex implements AutoCloseable {
	private final ActionMutexState.MutexMap mutexMap;
	private final @Nullable UUID playerId;

	private boolean closed;

	/* package-private */ ActionMutex(ActionMutexState.MutexMap mutexMap, @Nullable UUID playerId) {
		this.mutexMap = mutexMap;
		this.playerId = playerId;
	}

	public boolean isValid() {
		return !closed;
	}

	/* package-private */
	@Nullable UUID playerId() {
		return playerId;
	}

	@Override
	public void close() {
		if (closed) {
			return;
		}
		closed = true;
		mutexMap.release(this);
	}
}
