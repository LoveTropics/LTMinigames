package org.lovetropics.games.gametests.api;

import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.config.GameConfigs;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.lobbies.GameLobby;
import org.lovetropics.games.lobbies.LobbyGameQueue;
import org.lovetropics.games.lobbies.LobbyPlayerManager;
import org.lovetropics.games.lobbies.QueuedGame;

public record TestGameLobby(GameLobby lobby) {
	public LobbyPlayerManager getPlayers() {
		return lobby.getPlayers();
	}

	public LobbyGameQueue getGameQueue() {
		return lobby.getGameQueue();
	}

	public @Nullable IGamePhase getTopPhase() {
		return lobby.getTopPhase();
	}

	public QueuedGame enqueue(Identifier gameId) {
		return getGameQueue().enqueue(GameConfigs.REGISTRY.get(gameId));
	}
}
