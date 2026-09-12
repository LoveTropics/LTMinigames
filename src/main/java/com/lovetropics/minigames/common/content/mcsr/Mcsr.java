package com.lovetropics.minigames.common.content.mcsr;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.mcsr.behavior.PlayerWorldsBehavior;
import com.lovetropics.minigames.common.util.registry.GameBehaviorEntry;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;

/// Competitive Minecraft speedrunning mini-game
public class Mcsr {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final GameBehaviorEntry<PlayerWorldsBehavior> PLAYER_WORLDS = REGISTRATE.object("mcsr/player_worlds").behavior(PlayerWorldsBehavior.CODEC).register();

	public static void init() {
	}
}
