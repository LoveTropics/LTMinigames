package org.lovetropics.games.common.content.spleef;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.util.registry.GameBehaviorEntry;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;

public class Spleef {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final GameBehaviorEntry<SpleefBehavior> SPLEEF = REGISTRATE.object("spleef")
			.behavior(SpleefBehavior.CODEC)
			.register();

	public static void init() {
	}
}
