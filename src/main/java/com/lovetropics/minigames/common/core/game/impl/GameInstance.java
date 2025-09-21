package com.lovetropics.minigames.common.core.game.impl;

import com.lovetropics.minigames.common.core.game.IGame;
import com.lovetropics.minigames.common.core.game.IGameDefinition;
import com.lovetropics.minigames.common.core.game.lobby.IGameLobby;
import com.lovetropics.minigames.common.core.game.player.PlayerStorage;
import com.lovetropics.minigames.common.core.game.state.GameStateMap;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

/**
 * A unique instance of a specific minigame, stored in a GameLobby
 */
public final class GameInstance implements IGame {
	final GameLobby lobby;
	final MinecraftServer server;
	final IGameDefinition definition;

	final GameStateMap stateMap = new GameStateMap();

	final UUID uuid = UUID.randomUUID();

	final PlayerStorage playerStorage = new PlayerStorage();

	GameInstance(GameLobby lobby, IGameDefinition definition) {
		this.lobby = lobby;
		server = lobby.getServer();
		this.definition = definition;
	}

	@Override
	public IGameLobby lobby() {
		return lobby;
	}

	@Override
	public UUID gameUuid() {
		return uuid;
	}

	@Override
	public IGameDefinition definition() {
		return definition;
	}

	@Override
	public GameStateMap instanceState() {
		return stateMap;
	}

	public PlayerStorage getPlayerStorage() {
		return playerStorage;
	}
}
