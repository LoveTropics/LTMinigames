package org.lovetropics.games.common.core.integration.game_actions;

import net.minecraft.server.MinecraftServer;
import org.lovetropics.games.common.core.game.IGamePhase;

public interface GameAction {
	/// Resolves the requested action.
	///
	/// @param game The minigame that this occurred within
	/// @param server The game server the action is resolved on.
	/// @return Whether or not to send an acknowledgement back that
	/// the action has been resolved.
	boolean resolve(IGamePhase game, MinecraftServer server);
}
