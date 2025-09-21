package com.lovetropics.minigames.common.core.game;

import com.lovetropics.minigames.common.core.game.lobby.IGameLobby;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.state.GameStateMap;
import com.lovetropics.minigames.common.core.game.state.statistics.PlayerKey;
import com.lovetropics.minigames.common.core.integration.GameInstanceIntegrations;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

public interface IGame {
	IGameLobby lobby();

	UUID gameUuid();

	default MinecraftServer server() {
		return lobby().getServer();
	}

	default PlayerKey initiator() {
		return lobby().getMetadata().initiator();
	}

	default PlayerSet allPlayers() {
		return lobby().getPlayers();
	}

	IGameDefinition definition();

	GameStateMap instanceState();

	default GameInstanceIntegrations getIntegrationsOrThrow() {
		return instanceState().getOrThrow(GameInstanceIntegrations.KEY);
	}

	default RegistryAccess registryAccess() {
		return server().registryAccess();
	}
}
