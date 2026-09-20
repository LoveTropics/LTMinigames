package org.lovetropics.games.common.core.game.persistent.behavior.parkour;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;
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
