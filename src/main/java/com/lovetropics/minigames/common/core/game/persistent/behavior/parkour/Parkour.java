package com.lovetropics.minigames.common.core.game.persistent.behavior.parkour;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Item;

public class Parkour {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final ItemEntry<Item> PARKOUR_TELEPORTER = REGISTRATE.item("parkour_teleporter", Item::new)
			.lang("Parkour Checkpoint Teleporter")
			.register();

	public static void init() {
	    // load class
	}
}
