package org.lovetropics.games.gametests.api;

import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.config.GameConfigs;
import org.lovetropics.games.common.core.game.impl.GameLobby;
import org.lovetropics.games.common.core.game.impl.LobbyGameQueue;
import org.lovetropics.games.common.core.game.impl.LobbyPlayerManager;
import org.lovetropics.games.common.core.game.lobby.QueuedGame;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

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
