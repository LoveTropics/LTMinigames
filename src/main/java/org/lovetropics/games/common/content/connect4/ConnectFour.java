package org.lovetropics.games.common.content.connect4;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;

public class ConnectFour {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static void init() {
		REGISTRATE.registerBehavior("connect_four", ConnectFourBehavior.CODEC);
	}
}
