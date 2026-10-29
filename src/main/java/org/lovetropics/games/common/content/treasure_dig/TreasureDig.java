package org.lovetropics.games.common.content.treasure_dig;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;

public class TreasureDig {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static void init() {
		REGISTRATE.registerBehavior("treasure_dig", TreasureDigBehaviour.CODEC);
	}
}
