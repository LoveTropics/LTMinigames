package com.lovetropics.minigames.common.core.game.impl;

import com.lovetropics.minigames.common.core.game.IGameDefinition;
import com.lovetropics.minigames.common.core.game.state.GameStateMap;
import net.minecraft.server.MinecraftServer;

/**
 * A unique instance of a specific minigame, stored in a GameLobby
 */
public final class GameInstance {
	// TODO: Remove this backward reference
	final GameLobby lobby;
	final MinecraftServer server;
	final IGameDefinition definition;

	final GameStateMap stateMap = new GameStateMap();

	GameInstance(GameLobby lobby, IGameDefinition definition) {
		this.lobby = lobby;
		server = lobby.getServer();
		this.definition = definition;
	}

	public IGameDefinition definition() {
		return definition;
	}

	public GameStateMap instanceState() {
		return stateMap;
	}

	public MinecraftServer server() {
		return server;
	}
}
