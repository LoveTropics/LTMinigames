package com.lovetropics.minigames.common.core.game.impl;

import com.lovetropics.minigames.common.core.game.IGameDefinition;
import com.lovetropics.minigames.common.core.game.state.GameStateMap;
import net.minecraft.server.MinecraftServer;

/**
 * A unique instance of a specific minigame, stored in a GameLobby
 */
public final class GameInstance {
	// TODO: Remove this backward reference
	private final GameLobby lobby;
	private final MinecraftServer server;
	private final IGameDefinition definition;

	private final GameStateMap stateMap = new GameStateMap();

	GameInstance(GameLobby lobby, IGameDefinition definition) {
		this.lobby = lobby;
		server = lobby.getServer();
		this.definition = definition;
	}

	public MinecraftServer server() {
		return server;
	}

	public IGameDefinition definition() {
		return definition;
	}

	public GameStateMap instanceState() {
		return stateMap;
	}

	/* package-private */ GameLobby lobby() {
		return lobby;
	}
}
