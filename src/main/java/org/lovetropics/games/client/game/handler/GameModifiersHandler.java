package org.lovetropics.games.client.game.handler;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import org.jetbrains.annotations.Nullable;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.client.LTKeybinds;
import org.lovetropics.games.client.game.bingo.BingoBoardScreen;
import org.lovetropics.games.client.game.modifier.ModifiersScreen;
import org.lovetropics.games.client.game.shop.ShopScreen;
import org.lovetropics.games.common.content.mcsr.McsrTexts;
import org.lovetropics.games.common.core.game.client_state.instance.ModifiersClientState;

/// Shows a reminder of how to open the [ModifiersScreen]
@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public class GameModifiersHandler {
	private static final int OVERLAY_X = 20, OVERLAY_Y = 20;
	private static final int OVERLAY_GAP = 4;

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

	@SubscribeEvent
	static void registerLayers(RegisterGuiLayersEvent event) {
		event.registerAboveAll(Identifier.fromNamespaceAndPath(LoveTropics.ID, "rules_hint"), (guiGraphics, deltaTracker) -> {
			ModifiersClientState modifiers = GameModifiersHandler.modifiers;
			Screen screen = Minecraft.getInstance().gui.screen();
			if (modifiers == null || modifiers.entries().isEmpty() || screen instanceof ModifiersScreen || screen instanceof ShopScreen || screen instanceof BingoBoardScreen) {
				return;
			}

			Font font = Minecraft.getInstance().font;
			Component text = McsrTexts.OPEN_RULES_HINT.apply(LTKeybinds.OPEN_RULES.getTranslatedKeyMessage()).withStyle(ChatFormatting.GRAY);

			// Below the shop balance or the bingo board, if there are any
			int above = Math.max(GameShopHandler.getOverlayBottom(), GameBingoHandler.getOverlayBottom());
			int x = OVERLAY_X;
			int y = above >= 0 ? above + OVERLAY_GAP + 3 : OVERLAY_Y;

			guiGraphics.fill(x - 3, y - 3, x + font.width(text) + 3, y + font.lineHeight + 2, 0x66000000);
			guiGraphics.text(font, text, x, y, 0xFFFFFFFF, false);
		});
	}
}
