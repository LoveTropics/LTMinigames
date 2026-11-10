package org.lovetropics.games.lobbies.client.screen;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;

public final class ConfirmLayer {
	private ConfirmLayer() {
	}

	public static void open(Component title, Component message, Runnable onConfirm) {
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.gui.pushScreenLayer(new LayerScreen(confirmed -> {
			minecraft.gui.popScreenLayer();
			if (confirmed) {
				onConfirm.run();
			}
		}, title, message));
	}

	private static final class LayerScreen extends ConfirmScreen {
		LayerScreen(BooleanConsumer callback, Component title, Component message) {
			super(callback, title, message);
		}

		// blurring twice crashes the game...
		@Override
		public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
			extractTransparentBackground(graphics);
		}
	}
}
