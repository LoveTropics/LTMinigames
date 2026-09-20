package org.lovetropics.games.common.content.connect4;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.util.registry.GameBehaviorEntry;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;

public class ConnectFour {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final GameBehaviorEntry<ConnectFourBehavior> CONNECT_FOUR = REGISTRATE.object("connect_four")
			.behavior(ConnectFourBehavior.CODEC)
			.register();

	public static void init() {
	}
}
