package com.lovetropics.minigames.common.core.game.impl;

import com.lovetropics.minigames.client.lobby.state.ClientCurrentGame;
import com.lovetropics.minigames.common.core.game.GamePhaseType;
import com.lovetropics.minigames.common.core.game.IGameDefinition;
import com.lovetropics.minigames.common.core.game.lobby.LobbyControls;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;

import javax.annotation.Nullable;

final class LobbyStateManager {
	private final GameLobby lobby;
	private LobbyState state = new LobbyState.Paused();

	LobbyStateManager(GameLobby lobby) {
		this.lobby = lobby;
	}

	@Nullable
	public GameInstance getGame() {
		return state.phase != null ? state.phase.game : null;
	}

	@Nullable
	public GamePhase getTopPhase() {
		return state.phase;
	}

	@Nullable
	public ClientCurrentGame getClientCurrentGame() {
		return state.getClientCurrentGame();
	}

	public LobbyControls controls() {
		return state.controls;
	}

	@Nullable
	Change tick() {
		LobbyState newState = state.tick(lobby)
				.orElseGet(error -> errored(state, error));
		return trySetState(newState);
	}

	@Nullable
	Change handleError(Component error) {
		LobbyState state = errored(this.state, error);
		return trySetState(state);
	}

	@Nullable
	Change close() {
		return trySetState(new LobbyState.Closed());
	}

	@Nullable
	private Change trySetState(LobbyState newState) {
		LobbyState oldState = state;
		if (oldState == newState) {
			return null;
		}

		state = newState;
		return new Change(oldState.phase, newState.phase);
	}

	private LobbyState errored(LobbyState state, Component error) {
		LogManager.getLogger().info(error.getContents());
		GamePhase phase = state.phase;
		if (phase != null) {
			IGameDefinition definition = phase.definition();
			GamePhaseType phaseType = phase.phaseType();
			return new LobbyState.Errored(definition, phaseType, error);
		} else {
			return new LobbyState.Paused();
		}
	}

	record Change(@Nullable GamePhase oldPhase, @Nullable GamePhase newPhase) {
	}
}
