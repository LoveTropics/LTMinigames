package com.lovetropics.minigames.client;

import com.lovetropics.minigames.LoveTropics;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;

@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public class LTKeybinds {

	public static final KeyMapping.Category LT_CATEGORY = new KeyMapping.Category(LoveTropics.id("lovetropics"));

	public static final KeyMapping EXPAND_BINGO_BOARD = create("expand_bingo_board", InputConstants.KEY_B, KeyModifier.NONE);

	public static final KeyMapping JOIN = create("join", InputConstants.KEY_J, KeyModifier.CONTROL);
	public static final KeyMapping LEAVE = create("leave", InputConstants.KEY_L, KeyModifier.CONTROL);
	public static final KeyMapping MANAGE = create("manage", InputConstants.KEY_G, KeyModifier.CONTROL);

	public static void init() {
	}

	private static KeyMapping create(String id, int key, KeyModifier modifier) {
		final String modid = LoveTropics.ID;
		String description = "key." + modid + "." + id;
		return new KeyMapping(description, KeyConflictContext.IN_GAME, modifier, InputConstants.Type.KEYSYM, key, LT_CATEGORY);
	}


	@SubscribeEvent
	public static void registerBindings(RegisterKeyMappingsEvent event) {
		event.registerCategory(LT_CATEGORY);
		event.register(EXPAND_BINGO_BOARD);
		event.register(JOIN);
		event.register(LEAVE);
		event.register(MANAGE);
	}
}
