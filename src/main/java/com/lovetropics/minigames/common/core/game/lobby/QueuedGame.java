package com.lovetropics.minigames.common.core.game.lobby;

import com.lovetropics.minigames.common.core.game.config.GameConfig;

import java.util.concurrent.atomic.AtomicInteger;

/// Essentially the data underlying a queued game which is stored in a game lobby
public record QueuedGame(int networkId, GameConfig config) {
	private static final AtomicInteger NEXT_NETWORK_ID = new AtomicInteger();

	public static QueuedGame create(GameConfig game) {
		return new QueuedGame(NEXT_NETWORK_ID.getAndIncrement(), game);
	}
}
