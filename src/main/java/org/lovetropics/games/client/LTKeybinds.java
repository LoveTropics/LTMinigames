package org.lovetropics.games.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lovetropics.games.LoveTropics;

@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public class LTKeybinds {
	public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(LoveTropics.id("lovetropics"));

	public static final KeyMapping EXPAND_BINGO_BOARD = create("expand_bingo_board", InputConstants.KEY_B, KeyModifier.NONE);
	public static final KeyMapping OPEN_SHOP = create("open_shop", InputConstants.KEY_N, KeyModifier.NONE);
	public static final KeyMapping OPEN_RULES = create("open_rules", InputConstants.KEY_M, KeyModifier.NONE);

	private static KeyMapping create(String id, int key, KeyModifier modifier) {
		String description = "key." + LoveTropics.ID + "." + id;
		return new KeyMapping(description, KeyConflictContext.IN_GAME, modifier, InputConstants.Type.KEYSYM, key, CATEGORY);
	}

	@SubscribeEvent
	public static void registerBindings(RegisterKeyMappingsEvent event) {
		event.registerCategory(CATEGORY);
		event.register(EXPAND_BINGO_BOARD);
		event.register(OPEN_SHOP);
		event.register(OPEN_RULES);
	}
}
