package com.lovetropics.minigames.common.core.game.lobby;

import com.lovetropics.minigames.common.core.game.impl.GameLobby;
import net.minecraft.server.level.ServerPlayer;

public interface LobbyStateListener {
	static LobbyStateListener compose(LobbyStateListener... listeners) {
		return new LobbyStateListener() {
			@Override
			public void onPlayerJoin(GameLobby lobby, ServerPlayer player) {
				for (LobbyStateListener listener : listeners) {
					listener.onPlayerJoin(lobby, player);
				}
			}

			@Override
			public void onPlayerLeave(GameLobby lobby, ServerPlayer player) {
				for (LobbyStateListener listener : listeners) {
					listener.onPlayerLeave(lobby, player);
				}
			}

			@Override
			public void onPlayerStartTracking(GameLobby lobby, ServerPlayer player) {
				for (LobbyStateListener listener : listeners) {
					listener.onPlayerStartTracking(lobby, player);
				}
			}

			@Override
			public void onPlayerStopTracking(GameLobby lobby, ServerPlayer player) {
				for (LobbyStateListener listener : listeners) {
					listener.onPlayerStopTracking(lobby, player);
				}
			}

			@Override
			public void onLobbyStateChange(GameLobby lobby) {
				for (LobbyStateListener listener : listeners) {
					listener.onLobbyStateChange(lobby);
				}
			}

			@Override
			public void onLobbyNameChange(GameLobby lobby) {
				for (LobbyStateListener listener : listeners) {
					listener.onLobbyNameChange(lobby);
				}
			}

			@Override
			public void onLobbyPaused(GameLobby lobby) {
				for (LobbyStateListener listener : listeners) {
					listener.onLobbyPaused(lobby);
				}
			}

			@Override
			public void onLobbyStop(GameLobby lobby) {
				for (LobbyStateListener listener : listeners) {
					listener.onLobbyStop(lobby);
				}
			}

			@Override
			public void onGamePhaseChange(GameLobby lobby) {
				for (LobbyStateListener listener : listeners) {
					listener.onGamePhaseChange(lobby);
				}
			}
		};
	}

	default void onPlayerJoin(GameLobby lobby, ServerPlayer player) {
	}

	default void onPlayerLeave(GameLobby lobby, ServerPlayer player) {
	}

	default void onPlayerStartTracking(GameLobby lobby, ServerPlayer player) {
	}

	default void onPlayerStopTracking(GameLobby lobby, ServerPlayer player) {
	}

	default void onLobbyStateChange(GameLobby lobby) {
	}

	default void onLobbyNameChange(GameLobby lobby) {
	}

	default void onLobbyPaused(GameLobby lobby) {
	}

	default void onLobbyStop(GameLobby lobby) {
	}

	default void onGamePhaseChange(GameLobby lobby) {
	}
}
