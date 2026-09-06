package com.lovetropics.minigames.common.core.game.impl;

import com.lovetropics.minigames.client.lobby.state.ClientCurrentGame;
import com.lovetropics.minigames.common.core.game.IGameDefinition;
import com.lovetropics.minigames.common.core.game.lobby.LobbyControls;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

final class LobbyStateManager {
	private static final Logger LOGGER = LogUtils.getLogger();

	private final GameLobby lobby;
	private LobbyState state = new LobbyState.Paused();

	LobbyStateManager(GameLobby lobby) {
		this.lobby = lobby;
	}

	public @Nullable GamePhase getTopPhase() {
		return state.phase;
	}

	public @Nullable ClientCurrentGame getClientCurrentGame() {
		return state.getClientCurrentGame();
	}

	public LobbyControls controls() {
		return state.controls;
	}

	@Nullable Change tick() {
		LobbyState newState = state.tick(lobby)
				.orElseGet(error -> errored(state, error));
		return trySetState(newState);
	}

	@Nullable Change handleError(Component error) {
		LobbyState state = errored(this.state, error);
		return trySetState(state);
	}

	@Nullable Change close() {
		return trySetState(new LobbyState.Closed());
	}

	private @Nullable Change trySetState(LobbyState newState) {
		LobbyState oldState = state;
		if (oldState == newState) {
			return null;
		}

		state = newState;
		return new Change(oldState.phase, newState.phase);
	}

	private LobbyState errored(LobbyState state, Component error) {
		LOGGER.error("Encountered lobby error, pausing: {}", error.getString()); // Todo The main error message that causes this does not seem to passed to here currently
		GamePhase phase = state.phase;
		if (phase != null) {
			IGameDefinition definition = phase.definition();
			return new LobbyState.Errored(definition, error);
		} else {
			return new LobbyState.Paused();
		}
	}

	record Change(@Nullable GamePhase oldPhase, @Nullable GamePhase newPhase) {
	}
}
