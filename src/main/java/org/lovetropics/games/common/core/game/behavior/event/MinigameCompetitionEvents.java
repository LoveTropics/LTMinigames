package org.lovetropics.games.common.core.game.behavior.event;

public final class MinigameCompetitionEvents {
	public static final GameEventType<ReturnToLobby> RETURN_TO_LOBBY = GameEventType.create(ReturnToLobby.class, listeners -> () -> {
		for (ReturnToLobby listener : listeners) {
			listener.onReturnToLobby();
		}
	});

	private MinigameCompetitionEvents() {
	}

	public interface ReturnToLobby {
		void onReturnToLobby();
	}
}
