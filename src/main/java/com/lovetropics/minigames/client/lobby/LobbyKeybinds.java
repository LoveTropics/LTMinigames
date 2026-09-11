package com.lovetropics.minigames.client.lobby;

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
public class LobbyKeybinds {

	// Todo Should we Unifi this one
	public static final KeyMapping.Category LOBBY_CATEGORY = new KeyMapping.Category(LoveTropics.id("lobby"));

	public static final KeyMapping JOIN = create("join", InputConstants.KEY_J, KeyModifier.CONTROL);
	public static final KeyMapping LEAVE = create("leave", InputConstants.KEY_L, KeyModifier.CONTROL);
	public static final KeyMapping MANAGE = create("manage", InputConstants.KEY_G, KeyModifier.CONTROL);

	public static void init() {
	}

	private static KeyMapping create(String id, int key, KeyModifier modifier) {
		final String modid = LoveTropics.ID;
		String description = "key." + modid + "." + id;
		return new KeyMapping(description, KeyConflictContext.IN_GAME, modifier, InputConstants.Type.KEYSYM, key, LOBBY_CATEGORY);
	}


	@SubscribeEvent
	public static void registerBindings(RegisterKeyMappingsEvent event) {
		event.registerCategory(LOBBY_CATEGORY);
		event.register(JOIN);
		event.register(LEAVE);
		event.register(MANAGE);
	}
}
