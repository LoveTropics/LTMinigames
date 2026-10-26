package org.lovetropics.games.common.core.integration.game_actions;

import org.lovetropics.games.common.core.game.IGamePhase;

public interface GameAction {
	/// Attempts to apply the requested action.
	///
	/// @param game The minigame that this occurred within
	/// @return Whether to send an acknowledgement back that the action has been resolved.
	boolean apply(IGamePhase game);
}
