package com.lovetropics.minigames.common.core.game;

import com.lovetropics.minigames.common.core.game.lobby.GameLobbyMetadata;
import com.lovetropics.minigames.common.core.game.lobby.IGameLobby;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.state.GameStateMap;
import com.lovetropics.minigames.common.core.game.state.control.ControlCommandInvoker;
import com.lovetropics.minigames.common.core.game.state.control.ControlCommands;
import com.lovetropics.minigames.common.core.game.state.statistics.GameStatistics;
import com.lovetropics.minigames.common.core.game.state.statistics.PlayerKey;
import com.lovetropics.minigames.common.core.integration.GameInstanceIntegrations;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;
import java.util.concurrent.Executor;

public interface IGame extends Executor {
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

	default GameStatistics statistics() {
		return instanceState().get(GameStatistics.KEY);
	}

	default ControlCommands controlCommands() {
		return instanceState().get(ControlCommands.KEY);
	}

	default GameInstanceIntegrations getIntegrationsOrThrow() {
		return instanceState().getOrThrow(GameInstanceIntegrations.KEY);
	}

	default ControlCommandInvoker getControlInvoker() {
		ControlCommands commands = controlCommands();
		GameLobbyMetadata lobby = lobby().getMetadata();
		return ControlCommandInvoker.create(commands, lobby);
	}

	boolean isActive();

	@Override
	default void execute(final Runnable command) {
		if (isActive()) {
			server().execute(() -> {
				if (isActive()) {
					command.run();
				}
			});
		}
	}

    default RegistryAccess registryAccess() {
		return server().registryAccess();
	}
}
