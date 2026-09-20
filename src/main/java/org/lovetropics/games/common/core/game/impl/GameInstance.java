package org.lovetropics.games.common.core.game.impl;

import org.lovetropics.games.common.core.game.config.GameConfig;
import org.lovetropics.games.common.core.game.state.GameStateMap;
import net.minecraft.server.MinecraftServer;

/// A unique instance of a specific minigame, stored in a GameLobby
public final class GameInstance {
	// TODO: Remove this backward reference
	private final GameLobby lobby;
	private final MinecraftServer server;
	private final GameConfig config;

	private final GameStateMap stateMap = new GameStateMap();

	GameInstance(GameLobby lobby, GameConfig config) {
		this.lobby = lobby;
		server = lobby.getServer();
		this.config = config;
	}

	public MinecraftServer server() {
		return server;
	}

	public GameConfig config() {
		return config;
	}

	public GameStateMap instanceState() {
		return stateMap;
	}

	/* package-private */ GameLobby lobby() {
		return lobby;
	}
}
