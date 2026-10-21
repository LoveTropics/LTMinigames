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
import org.lovetropics.games.common.core.game.client_state.instance.ShopClientState;

/// Shows the player's tropicoins, and a reminder of how to open the [ShopScreen]
@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public class GameShopHandler {
	private static final int OVERLAY_X = 20, OVERLAY_Y = 20;
	private static final int OVERLAY_GAP = 4;

	@Nullable
	private static ShopClientState shop;

	static final ClientGameStateHandler<ShopClientState> HANDLER = new ClientGameStateHandler<>() {
		@Override
		public void accept(ShopClientState state) {
			shop = state;
		}

		@Override
		public void disable(ShopClientState state) {
			shop = null;
		}
	};

	public static @Nullable ShopClientState shop() {
		return shop;
	}

	/// Below the bingo board, if there is one
	private static int getOverlayY() {
		int bingoBottom = GameBingoHandler.getOverlayBottom();
		return bingoBottom >= 0 ? bingoBottom + OVERLAY_GAP + 3 : OVERLAY_Y;
	}

	/// @return the y position just below the balance, or `-1` if it isn't shown
	public static int getOverlayBottom() {
		if (shop == null) {
			return -1;
		}
		return getOverlayY() + Minecraft.getInstance().font.lineHeight + 2;
	}

	@SubscribeEvent
	static void onClientTick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		while (LTKeybinds.OPEN_SHOP.consumeClick()) {
			if (shop != null && minecraft.gui.screen() == null) {
				minecraft.setScreenAndShow(new ShopScreen());
			}
		}
	}

	@SubscribeEvent
	static void registerLayers(RegisterGuiLayersEvent event) {
		event.registerAboveAll(Identifier.fromNamespaceAndPath(LoveTropics.ID, "shop_balance"), (guiGraphics, deltaTracker) -> {
			ShopClientState shop = GameShopHandler.shop;
			Screen screen = Minecraft.getInstance().gui.screen();
			if (shop == null || screen instanceof ShopScreen || screen instanceof BingoBoardScreen || screen instanceof ModifiersScreen) {
				return;
			}

			Font font = Minecraft.getInstance().font;
			Component text = McsrTexts.TROPICOINS.apply(shop.balance()).withStyle(ChatFormatting.GOLD)
					.append(Component.literal(" - ").withStyle(ChatFormatting.GRAY))
					.append(McsrTexts.OPEN_SHOP_HINT.apply(LTKeybinds.OPEN_SHOP.getTranslatedKeyMessage()).withStyle(ChatFormatting.GRAY));

			int x = OVERLAY_X;
			int y = getOverlayY();

			guiGraphics.fill(x - 3, y - 3, x + font.width(text) + 3, y + font.lineHeight + 2, 0x66000000);
			guiGraphics.text(font, text, x, y, 0xFFFFFFFF, false);
		});
	}
}
