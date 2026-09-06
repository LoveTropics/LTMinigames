package com.lovetropics.minigames.common.core.game.lobby;

import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.state.statistics.PlayerKey;

import org.jspecify.annotations.Nullable;

public record GameLobbyMetadata(GameLobbyId id, PlayerKey initiator, String name, LobbyVisibility visibility) {
	public GameLobbyMetadata(GameLobbyId id, PlayerKey initiator, String name) {
		this(id, initiator, name, LobbyVisibility.PRIVATE);
	}

	public String joinCommand(@Nullable PlayerRole role) {
		String command = "/game join " + id.uuid();
		if (role != null) {
			command += " as " + role.getKey();
		}
		return command;
	}

	public GameLobbyMetadata withName(String name) {
		return new GameLobbyMetadata(id, initiator, name, visibility);
	}

	public GameLobbyMetadata withVisibility(LobbyVisibility visibility) {
		return new GameLobbyMetadata(id, initiator, name, visibility);
	}
}
