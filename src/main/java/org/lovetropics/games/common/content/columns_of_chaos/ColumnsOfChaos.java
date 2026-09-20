package org.lovetropics.games.common.content.columns_of_chaos;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.util.registry.GameBehaviorEntry;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;

public class ColumnsOfChaos {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final GameBehaviorEntry<ColumnsOfChaosBehavior> BEHAVIOR = REGISTRATE.object("columns_of_chaos")
			.behavior(ColumnsOfChaosBehavior.CODEC)
			.register();

	public static void init() {
	}
}
