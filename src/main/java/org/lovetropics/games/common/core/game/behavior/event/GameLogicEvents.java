package org.lovetropics.games.common.core.game.behavior.event;

import org.lovetropics.games.common.core.game.GameWinner;
import org.lovetropics.games.common.core.game.state.team.GameTeam;
import net.minecraft.server.level.ServerPlayer;

public final class GameLogicEvents {
	public static final GameEventType<GameOver> GAME_OVER = GameEventType.create(GameOver.class, listeners -> winner -> {
		for (GameOver listener : listeners) {
			listener.onGameOver(winner);
		}
	});

	public static final GameEventType<GameTimeRanOut> REQUEST_GAME_OVER = GameEventType.create(GameTimeRanOut.class, listeners -> () -> {
		for (GameTimeRanOut listener : listeners) {
			if (listener.requestGameOver()) {
				return true;
			}
		}
		return false;
	});

	private GameLogicEvents() {
	}

	public interface GameTimeRanOut {
		boolean requestGameOver();
	}

	public interface GameOver {
		void onGameOver(GameWinner winner);

		default void onGameWonBy(ServerPlayer player) {
			onGameOver(new GameWinner.Player(player));
		}

		default void onGameWonBy(GameTeam team) {
			onGameOver(new GameWinner.Team(team));
		}
	}
}
