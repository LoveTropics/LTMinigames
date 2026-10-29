package org.lovetropics.games.common.content.drr_showdown;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;

public class DDRShowdown {

	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static void init() {
		REGISTRATE.registerBehavior("ddr_showdown", DDRShowdownBehavior.CODEC);
	}

}
