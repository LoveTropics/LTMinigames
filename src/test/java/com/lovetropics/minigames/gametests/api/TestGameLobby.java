package com.lovetropics.minigames.gametests.api;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.config.GameConfigs;
import com.lovetropics.minigames.common.core.game.impl.GameLobby;
import com.lovetropics.minigames.common.core.game.impl.LobbyGameQueue;
import com.lovetropics.minigames.common.core.game.impl.LobbyPlayerManager;
import com.lovetropics.minigames.common.core.game.lobby.QueuedGame;
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
