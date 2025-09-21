package com.lovetropics.minigames.common.content.spleef;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.util.registry.GameBehaviorEntry;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;

public class Spleef {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final GameBehaviorEntry<SpleefBehavior> SPLEEF = REGISTRATE.object("spleef")
			.behavior(SpleefBehavior.CODEC)
			.register();

	public static void init() {
	}
}
