package com.lovetropics.minigames.common.core.item;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Item;

public class MinigameItems {

	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final ItemEntry<EditRegionItem> EDIT_REGION = REGISTRATE.item("edit_region", EditRegionItem::new)
			.register();

	/** Powerups */
	public static final ItemEntry<Item> UPSET_STOMACH = REGISTRATE.item("upset_stomach", Item::new).register();
	public static final ItemEntry<Item> COLADARAL_DAMAGE = REGISTRATE.item("coladaral_damage", Item::new).register();
	public static final ItemEntry<Item> TAPIR_TAKEOVER = REGISTRATE.item("tapir_takeover", Item::new).register();

	/** End powerups */

	public static void init() {
	}
}
