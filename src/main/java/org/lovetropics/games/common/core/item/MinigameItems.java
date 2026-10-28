package org.lovetropics.games.common.core.item;

import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.Consumable;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;

public class MinigameItems {

	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	/// Powerups and Sabotages
	// Sabotages
	public static final ItemEntry<PowerupItem> UPSET_STOMACH = REGISTRATE.item("upset_stomach", PowerupItem::new).properties(MinigameItems::createPowerupItemProperties).register();
	public static final ItemEntry<PowerupItem> COLADARAL_DAMAGE = REGISTRATE.item("coladaral_damage", PowerupItem::new).properties(MinigameItems::createPowerupItemProperties).lang("Colada-ral Damage").register();
	public static final ItemEntry<PowerupItem> TAPIR_TAKEOVER = REGISTRATE.item("tapir_takeover", PowerupItem::new).properties(MinigameItems::createPowerupItemProperties).register();
	public static final ItemEntry<PowerupItem> SHRUGGY_ARMS = REGISTRATE.item("shruggy_arms", PowerupItem::new).properties(MinigameItems::createPowerupItemProperties).register();
	public static final ItemEntry<PowerupItem> CONTROL_INVERTER = REGISTRATE.item("control_inverter", PowerupItem::new).properties(MinigameItems::createPowerupItemProperties).register();

	// Powerups
	public static final ItemEntry<PowerupItem> ENDER_ARMS = REGISTRATE.item("ender_arms", PowerupItem::new).properties(MinigameItems::createPowerupItemProperties).register();

	/// End powerups and sabotages
	public static void init() {
	}

	private static Item.Properties createPowerupItemProperties(Item.Properties properties) {
		properties.component(DataComponents.CONSUMABLE, Consumable.builder().consumeSeconds((float) 0.05).animation(ItemUseAnimation.TOOT_HORN).build());
		return properties;
	}

}
