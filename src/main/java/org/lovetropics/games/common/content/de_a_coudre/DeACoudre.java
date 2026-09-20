package org.lovetropics.games.common.content.de_a_coudre;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.util.registry.GameBehaviorEntry;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;

public class DeACoudre {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final GameBehaviorEntry<DeACoudreBehavior> BEHAVIOR = REGISTRATE.object("de_a_coudre")
			.behavior(DeACoudreBehavior.CODEC)
			.register();

	public static void init() {
	}
}
