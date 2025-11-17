package com.lovetropics.minigames.common.core.item;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;

public class MinigameItems {

	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final ItemEntry<EditRegionItem> EDIT_REGION = REGISTRATE.item("edit_region", EditRegionItem::new)
			.register();

	/** Powerups and Sabotages */
	// Sabotages
	public static final ItemEntry<PowerupItem> UPSET_STOMACH = REGISTRATE.item("upset_stomach", PowerupItem::new).register();
	public static final ItemEntry<PowerupItem> COLADARAL_DAMAGE = REGISTRATE.item("coladaral_damage", PowerupItem::new).register();
	public static final ItemEntry<PowerupItem> TAPIR_TAKEOVER = REGISTRATE.item("tapir_takeover", PowerupItem::new).register();
	public static final ItemEntry<PowerupItem> SHRUGGY_ARMS = REGISTRATE.item("shruggy_arms", PowerupItem::new).register();
	public static final ItemEntry<PowerupItem> CONTROL_INVERTER = REGISTRATE.item("control_inverter", PowerupItem::new).register();

	// Powerups
	public static final ItemEntry<PowerupItem> ENDER_ARMS = REGISTRATE.item("ender_arms", PowerupItem::new).register();

	/** End powerups and sabotages */

	public static void init() {
	}
}
