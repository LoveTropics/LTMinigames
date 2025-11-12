package com.lovetropics.minigames.common.core.game.impl;

import com.lovetropics.minigames.common.core.game.IGameDefinition;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.player.PlayerStorage;
import com.lovetropics.minigames.common.core.game.state.GameStateMap;
import net.minecraft.server.MinecraftServer;

/**
 * A unique instance of a specific minigame, stored in a GameLobby
 */
public final class GameInstance {
	final GameLobby lobby;
	final MinecraftServer server;
	final IGameDefinition definition;

	final GameStateMap stateMap = new GameStateMap();

	final PlayerStorage playerStorage = new PlayerStorage();

	GameInstance(GameLobby lobby, IGameDefinition definition) {
		this.lobby = lobby;
		server = lobby.getServer();
		this.definition = definition;
	}

	public GameLobby lobby() {
		return lobby;
	}

	public IGameDefinition definition() {
		return definition;
	}

	public GameStateMap instanceState() {
		return stateMap;
	}

	public PlayerStorage getPlayerStorage() {
		return playerStorage;
	}

	public MinecraftServer server() {
		return lobby.getServer();
	}

	public PlayerSet allPlayers() {
		return lobby.getPlayers();
	}
}
