package org.lovetropics.games.common.content.treasure_dig;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.util.registry.GameBehaviorEntry;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;

public class TreasureDig {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final GameBehaviorEntry<TreasureDigBehaviour> BEHAVIOR = REGISTRATE.object("treasure_dig")
			.behavior(TreasureDigBehaviour.CODEC)
			.register();

	public static void init() {
	}
}
