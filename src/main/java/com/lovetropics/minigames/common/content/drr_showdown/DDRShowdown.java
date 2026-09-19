package com.lovetropics.minigames.common.content.drr_showdown;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.util.registry.GameBehaviorEntry;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;

public class DDRShowdown {

	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final GameBehaviorEntry<DDRShowdownBehavior> DDR_SHOWDOWN = REGISTRATE.object("ddr_showdown")
			.behavior(DDRShowdownBehavior.CODEC)
			.register();


	public static void init() {
	}

}
