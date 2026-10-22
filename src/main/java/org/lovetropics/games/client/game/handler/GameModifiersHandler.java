package org.lovetropics.games.client.game.handler;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.client.LTKeybinds;
import org.lovetropics.games.client.game.modifier.ModifiersScreen;
import org.lovetropics.games.common.core.game.client_state.instance.ModifiersClientState;

/// Opens the [ModifiersScreen]. How to open it is shown by [GameControlsHud].
@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public class GameModifiersHandler {
	@Nullable
	private static ModifiersClientState modifiers;

	static final ClientGameStateHandler<ModifiersClientState> HANDLER = new ClientGameStateHandler<>() {
		@Override
		public void accept(ModifiersClientState state) {
			modifiers = state;
		}

		@Override
		public void disable(ModifiersClientState state) {
			modifiers = null;
		}
	};

	public static @Nullable ModifiersClientState modifiers() {
		return modifiers;
	}

	@SubscribeEvent
	static void onClientTick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		while (LTKeybinds.OPEN_RULES.consumeClick()) {
			if (modifiers != null && minecraft.gui.screen() == null) {
				minecraft.setScreenAndShow(new ModifiersScreen());
			}
		}
	}
}
