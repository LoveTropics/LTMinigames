package com.lovetropics.minigames.common.content.columns_of_chaos;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.util.registry.GameBehaviorEntry;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;

public class ColumnsOfChaos {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final GameBehaviorEntry<ColumnsOfChaosBehavior> BEHAVIOR = REGISTRATE.object("columns_of_chaos")
			.behavior(ColumnsOfChaosBehavior.CODEC)
			.register();

	public static void init() {
	}
}
