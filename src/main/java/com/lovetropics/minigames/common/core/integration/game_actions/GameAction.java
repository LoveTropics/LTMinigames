package com.lovetropics.minigames.common.core.integration.game_actions;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import net.minecraft.server.MinecraftServer;

public interface GameAction {
	/**
	 * Resolves the requested action.
	 *
	 * @param game The minigame that this occurred within
	 * @param server The game server the action is resolved on.
	 * @return Whether or not to send an acknowledgement back that
	 * the action has been resolved.
	 */
	boolean resolve(IGamePhase game, MinecraftServer server);
}
