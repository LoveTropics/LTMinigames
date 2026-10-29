package org.lovetropics.games.common.content.de_a_coudre;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;

public class DeACoudre {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static void init() {
		REGISTRATE.registerBehavior("de_a_coudre", DeACoudreBehavior.CODEC);
	}
}
