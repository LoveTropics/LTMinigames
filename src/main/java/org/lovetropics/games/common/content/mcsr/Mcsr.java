package org.lovetropics.games.common.content.mcsr;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.content.mcsr.behavior.PlayerWorldsBehavior;
import org.lovetropics.games.common.util.registry.GameBehaviorEntry;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;

/// Competitive Minecraft speedrunning mini-game
public class Mcsr {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final GameBehaviorEntry<PlayerWorldsBehavior> PLAYER_WORLDS = REGISTRATE.object("mcsr/player_worlds").behavior(PlayerWorldsBehavior.CODEC).register();

	public static void init() {
	}
}
