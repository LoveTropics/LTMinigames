package com.lovetropics.minigames.common.content.connect4;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.util.registry.GameBehaviorEntry;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;

public class ConnectFour {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final GameBehaviorEntry<ConnectFourBehavior> CONNECT_FOUR = REGISTRATE.object("connect_four")
			.behavior(ConnectFourBehavior.CODEC)
			.register();

	public static void init() {
	}
}
