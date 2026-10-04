package org.lovetropics.games.common.core.game.persistent.behavior.parkour;

import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Item;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;

public class Parkour {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final ItemEntry<Item> PARKOUR_TELEPORTER = REGISTRATE.item("parkour_teleporter", Item::new)
			.lang("Parkour Checkpoint Teleporter")
			.register();

	public static void init() {
	    // load class
	}
}
