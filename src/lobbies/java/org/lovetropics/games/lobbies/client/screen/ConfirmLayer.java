package org.lovetropics.games.lobbies.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;

public final class ConfirmLayer {
	private ConfirmLayer() {
	}

	public static void open(Component title, Component message, Runnable onConfirm) {
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.gui.pushScreenLayer(new ConfirmScreen(confirmed -> {
			minecraft.gui.popScreenLayer();
			if (confirmed) {
				onConfirm.run();
			}
		}, title, message));
	}
}
