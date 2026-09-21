package org.lovetropics.games.lobbies;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lovetropics.games.client.LTKeybinds;
import org.lovetropics.games.common.core.game.util.TranslationCollector;

@EventBusSubscriber(modid = LobbiesMod.ID, value = Dist.CLIENT)
public class LobbyKeybinds {
	public static final TranslationCollector TRANSLATIONS = new TranslationCollector("key." + LobbiesMod.ID + ".");

	public static final KeyMapping JOIN = create("join", "Join Lobby", InputConstants.KEY_J, KeyModifier.CONTROL);
	public static final KeyMapping LEAVE = create("leave", "Leave Lobby", InputConstants.KEY_L, KeyModifier.CONTROL);
	public static final KeyMapping MANAGE = create("manage", "Manage Lobby", InputConstants.KEY_G, KeyModifier.CONTROL);

	private static KeyMapping create(String id, String name, int key, KeyModifier modifier) {
		String translationKey = TRANSLATIONS.addKey(id, name);
		return new KeyMapping(translationKey, KeyConflictContext.IN_GAME, modifier, InputConstants.Type.KEYSYM, key, LTKeybinds.CATEGORY);
	}

	@SubscribeEvent
	public static void registerBindings(RegisterKeyMappingsEvent event) {
		event.register(JOIN);
		event.register(LEAVE);
		event.register(MANAGE);
	}
}
