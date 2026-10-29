package org.lovetropics.games.common.content.build_battle;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;

public class BuildBattle {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static void init() {
		REGISTRATE.registerBehavior("build_battle", BuildBattleBehavior.CODEC);
	}
}
