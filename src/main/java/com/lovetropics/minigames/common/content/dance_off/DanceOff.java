package com.lovetropics.minigames.common.content.dance_off;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.util.registry.GameBehaviorEntry;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;

public class DanceOff {

	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final GameBehaviorEntry<DanceOffBehavior> DANCE_OFF = REGISTRATE.object("dance_off")
			.behavior(DanceOffBehavior.CODEC)
			.register();


	public static void init() {
	}

}
