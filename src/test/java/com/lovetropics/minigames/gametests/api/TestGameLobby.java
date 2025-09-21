package com.lovetropics.minigames.gametests.api;

import com.lovetropics.minigames.client.lobby.state.ClientCurrentGame;
import com.lovetropics.minigames.common.core.game.IGame;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.config.GameConfigs;
import com.lovetropics.minigames.common.core.game.lobby.GameLobbyMetadata;
import com.lovetropics.minigames.common.core.game.lobby.IGameLobby;
import com.lovetropics.minigames.common.core.game.lobby.IGameLobbyPlayers;
import com.lovetropics.minigames.common.core.game.lobby.ILobbyGameQueue;
import com.lovetropics.minigames.common.core.game.lobby.ILobbyManagement;
import com.lovetropics.minigames.common.core.game.lobby.LobbyControls;
import com.lovetropics.minigames.common.core.game.lobby.QueuedGame;
import com.lovetropics.minigames.common.core.game.player.PlayerIterable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

public record TestGameLobby(IGameLobby delegate) implements IGameLobby {
	@Override
	public MinecraftServer getServer() {
		return delegate.getServer();
	}

	@Override
	public GameLobbyMetadata getMetadata() {
		return delegate.getMetadata();
	}

	@Override
	public IGameLobbyPlayers getPlayers() {
		return delegate.getPlayers();
	}

	@Override
	public ILobbyGameQueue getGameQueue() {
		return delegate.getGameQueue();
	}

	@Override
	@Nullable
	public IGame getCurrentGame() {
		return delegate.getCurrentGame();
	}

	@Nullable
	@Override
	public IGamePhase getTopPhase() {
		return delegate.getTopPhase();
	}

	@Override
	@Nullable
	public IGamePhase getActivePhase() {
		return delegate.getActivePhase();
	}

	@Override
	@Nullable
	public ClientCurrentGame getClientCurrentGame() {
		return delegate.getClientCurrentGame();
	}

	@Override
	public LobbyControls getControls() {
		return delegate.getControls();
	}

	@Override
	public ILobbyManagement getManagement() {
		return delegate.getManagement();
	}

	@Override
	public PlayerIterable getTrackingPlayers() {
		return delegate.getTrackingPlayers();
	}

	@Override
	public boolean isVisibleTo(CommandSourceStack source) {
		return delegate.isVisibleTo(source);
	}

	@Override
	public boolean isVisibleTo(ServerPlayer player) {
		return delegate.isVisibleTo(player);
	}

	public QueuedGame enqueue(ResourceLocation gameId) {
		return getGameQueue().enqueue(GameConfigs.REGISTRY.get(gameId));
	}
}
