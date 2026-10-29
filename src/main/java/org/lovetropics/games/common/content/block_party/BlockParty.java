package org.lovetropics.games.common.content.block_party;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;

public final class BlockParty {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static void init() {
		REGISTRATE.registerBehavior("block_party", BlockPartyBehavior.CODEC);
	}
}
