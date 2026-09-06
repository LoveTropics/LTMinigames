package com.lovetropics.minigames.client.lobby.state;

import com.lovetropics.minigames.common.core.game.LobbyStatus;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import org.jspecify.annotations.Nullable;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public class ClientLobbyState {
	final int id;

	String name;
	final Set<UUID> players = new ObjectOpenHashSet<>();

	@Nullable ClientCurrentGame currentGame;

	ClientLobbyState(int id) {
		this.id = id;
	}

	public void update(String name, @Nullable ClientCurrentGame currentGame) {
		this.name = name;
		this.currentGame = currentGame;
	}

	public void setPlayers(Collection<UUID> players) {
		this.players.clear();
		this.players.addAll(players);
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public @Nullable ClientCurrentGame getCurrentGame() {
		return currentGame;
	}

	public Set<UUID> getPlayers() {
		return players;
	}

	public int getPlayerCount() {
		return players.size();
	}

	public LobbyStatus getStatus() {
		if (currentGame != null) {
			if (currentGame.error().isPresent()) {
				return LobbyStatus.PAUSED;
			}
			return switch (currentGame.phase()) {
				case PLAYING -> LobbyStatus.PLAYING;
				case WAITING -> LobbyStatus.WAITING;
			};
		}
		return LobbyStatus.PAUSED;
	}
}
