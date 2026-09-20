package org.lovetropics.games.common.content.block_party;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.util.registry.GameBehaviorEntry;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;

public final class BlockParty {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final GameBehaviorEntry<BlockPartyBehavior> BLOCK_PARTY = REGISTRATE.object("block_party")
			.behavior(BlockPartyBehavior.CODEC)
			.register();

	public static void init() {
	}
}
