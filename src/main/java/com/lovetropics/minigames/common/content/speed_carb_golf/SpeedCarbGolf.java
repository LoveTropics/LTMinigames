package com.lovetropics.minigames.common.content.speed_carb_golf;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.util.registry.GameBehaviorEntry;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;

public class SpeedCarbGolf {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final GameBehaviorEntry<SpeedCarbGolfBehaviour> BEHAVIOR = REGISTRATE.object("speed_carb_golf")
			.behavior(SpeedCarbGolfBehaviour.CODEC)
			.register();

	public static void init() {
	}
}
