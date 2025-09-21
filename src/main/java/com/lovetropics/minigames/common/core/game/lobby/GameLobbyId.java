package com.lovetropics.minigames.common.core.game.lobby;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public record GameLobbyId(UUID uuid, int networkId) {
	private static final AtomicInteger NETWORK_ID = new AtomicInteger();

	public static GameLobbyId next() {
		return new GameLobbyId(UUID.randomUUID(), NETWORK_ID.getAndIncrement());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}

		return uuid.equals(((GameLobbyId) obj).uuid);
	}

	@Override
	public int hashCode() {
		return uuid.hashCode();
	}
}
