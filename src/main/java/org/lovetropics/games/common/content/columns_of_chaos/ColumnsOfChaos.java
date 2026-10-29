package org.lovetropics.games.common.content.columns_of_chaos;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;

public class ColumnsOfChaos {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static void init() {
		REGISTRATE.registerBehavior("columns_of_chaos", ColumnsOfChaosBehavior.CODEC);
	}
}
