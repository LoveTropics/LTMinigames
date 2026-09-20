package org.lovetropics.games.common.content.build_battle;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.util.registry.GameBehaviorEntry;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;

public class BuildBattle {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final GameBehaviorEntry<BuildBattleBehavior> BUILD_BATTLE = REGISTRATE.object("build_battle")
			.behavior(BuildBattleBehavior.CODEC)
			.register();

	public static void init() {
	}
}
