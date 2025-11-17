package com.lovetropics.minigames.common.content.build_battle;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.util.registry.GameBehaviorEntry;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;

public class BuildBattle {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final GameBehaviorEntry<BuildBattleBehavior> BUILD_BATTLE = REGISTRATE.object("build_battle")
			.behavior(BuildBattleBehavior.CODEC)
			.register();

	public static void init() {
	}
}
