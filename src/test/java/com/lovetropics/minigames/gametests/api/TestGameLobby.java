package com.lovetropics.minigames.gametests.api;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.config.GameConfigs;
import com.lovetropics.minigames.common.core.game.impl.GameLobby;
import com.lovetropics.minigames.common.core.game.impl.LobbyGameQueue;
import com.lovetropics.minigames.common.core.game.impl.LobbyPlayerManager;
import com.lovetropics.minigames.common.core.game.lobby.QueuedGame;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public record TestGameLobby(GameLobby lobby) {
	public LobbyPlayerManager getPlayers() {
		return lobby.getPlayers();
	}

	public LobbyGameQueue getGameQueue() {
		return lobby.getGameQueue();
	}

	@Nullable
	public IGamePhase getActivePhase() {
		return lobby.getActivePhase();
	}

	public QueuedGame enqueue(ResourceLocation gameId) {
		return getGameQueue().enqueue(GameConfigs.REGISTRY.get(gameId));
	}
}
