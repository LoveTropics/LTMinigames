package org.lovetropics.games.common.content.speed_carb_golf;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.util.registry.GameBehaviorEntry;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;

public class SpeedCarbGolf {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final GameBehaviorEntry<SpeedCarbGolfBehaviour> BEHAVIOR = REGISTRATE.object("speed_carb_golf")
			.behavior(SpeedCarbGolfBehaviour.CODEC)
			.register();

	public static void init() {
	}
}
