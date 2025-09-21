package com.lovetropics.minigames.common.content.treasure_dig;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.util.registry.GameBehaviorEntry;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;

public class TreasureDig {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final GameBehaviorEntry<TreasureDigBehaviour> BEHAVIOR = REGISTRATE.object("treasure_dig")
			.behavior(TreasureDigBehaviour.CODEC)
			.register();

	public static void init() {
	}
}
