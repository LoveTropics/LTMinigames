package org.lovetropics.games.common.content.drr_showdown;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.util.registry.GameBehaviorEntry;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;

public class DDRShowdown {

	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final GameBehaviorEntry<DDRShowdownBehavior> DDR_SHOWDOWN = REGISTRATE.object("ddr_showdown")
			.behavior(DDRShowdownBehavior.CODEC)
			.register();


	public static void init() {
	}

}
