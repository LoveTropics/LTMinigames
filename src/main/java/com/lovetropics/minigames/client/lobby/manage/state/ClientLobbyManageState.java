package com.lovetropics.minigames.client.lobby.manage.state;

import com.lovetropics.minigames.client.lobby.state.ClientCurrentGame;
import com.lovetropics.minigames.client.lobby.state.ClientGameDefinition;
import com.lovetropics.minigames.common.core.game.lobby.LobbyControls;
import com.lovetropics.minigames.common.core.game.lobby.LobbyVisibility;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntList;

import org.jspecify.annotations.Nullable;
import java.util.List;

public final class ClientLobbyManageState {
	private String name = "";
	private @Nullable ClientCurrentGame currentGame;
	private ClientLobbyQueue queue = new ClientLobbyQueue();
	private List<ClientLobbyPlayer> players = List.of();
	private LobbyControls.State controlsState = LobbyControls.State.disabled();
	private LobbyVisibility visibility = LobbyVisibility.PRIVATE;
	private boolean canFocusLive;

	private List<ClientGameDefinition> installedGames = List.of();

	public String getName() {
		return name;
	}

	public @Nullable ClientCurrentGame getCurrentGame() {
		return currentGame;
	}

	public ClientLobbyQueue getQueue() {
		return queue;
	}

	public List<ClientLobbyPlayer> getPlayers() {
		return players;
	}

	public LobbyControls.State getControlsState() {
		return controlsState;
	}

	public LobbyVisibility getVisibility() {
		return visibility;
	}

	public boolean canFocusLive() {
		return canFocusLive;
	}

	public List<ClientGameDefinition> getInstalledGames() {
		return installedGames;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setCurrentGame(@Nullable ClientCurrentGame game) {
		currentGame = game;
	}

	public void setQueue(ClientLobbyQueue queue) {
		this.queue = queue;
	}

	public void setInstalledGames(List<ClientGameDefinition> installedGames) {
		this.installedGames = installedGames;
	}

	public void updateQueue(IntList queue, Int2ObjectMap<ClientLobbyQueuedGame> updated) {
		this.queue.applyUpdates(queue, updated);
	}

	public void setPlayers(List<ClientLobbyPlayer> players) {
		this.players = players;
	}

	public void setControlsState(LobbyControls.State state) {
		controlsState = state;
	}

	public void setVisibility(LobbyVisibility visibility, boolean canFocusLive) {
		this.visibility = visibility;
		this.canFocusLive = canFocusLive;
	}
}
