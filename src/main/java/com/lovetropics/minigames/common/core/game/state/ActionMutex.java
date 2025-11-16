package com.lovetropics.minigames.common.core.game.state;

import javax.annotation.Nullable;
import java.util.UUID;

public class ActionMutex implements AutoCloseable {
	private final ActionMutexState.MutexMap mutexMap;
	@Nullable
	private final UUID playerId;

	private boolean closed;

	/* package-private */ ActionMutex(ActionMutexState.MutexMap mutexMap, @Nullable UUID playerId) {
		this.mutexMap = mutexMap;
		this.playerId = playerId;
	}

	public boolean isValid() {
		return !closed;
	}

	@Nullable
	/* package-private */ UUID playerId() {
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
