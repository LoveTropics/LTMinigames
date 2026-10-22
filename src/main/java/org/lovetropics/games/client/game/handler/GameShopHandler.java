package org.lovetropics.games.client.game.handler;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.client.LTKeybinds;
import org.lovetropics.games.client.game.shop.ShopScreen;
import org.lovetropics.games.common.core.game.client_state.instance.ShopClientState;

/// Opens the [ShopScreen]. The player's tropicoins are shown by [GameControlsHud].
@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public class GameShopHandler {
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

	@SubscribeEvent
	static void onClientTick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		while (LTKeybinds.OPEN_SHOP.consumeClick()) {
			if (shop != null && minecraft.gui.screen() == null) {
				minecraft.setScreenAndShow(new ShopScreen());
			}
		}
	}
}
