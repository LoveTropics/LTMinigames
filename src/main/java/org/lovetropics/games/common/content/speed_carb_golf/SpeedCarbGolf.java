package org.lovetropics.games.common.content.speed_carb_golf;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;

public class SpeedCarbGolf {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static void init() {
		REGISTRATE.registerBehavior("speed_carb_golf", SpeedCarbGolfBehaviour.CODEC);
	}
}
