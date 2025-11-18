package com.lovetropics.minigames.client.gui;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.network.ClientboundFadeToBlackPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(Dist.CLIENT)
public class ClientFadeToBlack {
	private static boolean enabled;
	private static float speed = 1.0f;
	private static float lastAlpha;
	private static float alpha;

	@SubscribeEvent
	public static void tick(ClientTickEvent.Post event) {
		lastAlpha = alpha;
		if (enabled) {
			alpha = Math.min(alpha + speed, 1.0f);
		} else if (!isLoadingScreen(Minecraft.getInstance().screen)) {
			alpha = Math.max(alpha - speed, 0.0f);
		}
	}

	@SubscribeEvent
	public static void registerGuiLayers(RegisterGuiLayersEvent event) {
		event.registerAboveAll(LoveTropics.location("fade_to_black"), (graphics, deltaTracker) -> {
			if (!isLoadingScreen(Minecraft.getInstance().screen)) {
				draw(graphics, deltaTracker.getGameTimeDeltaPartialTick(true));
			}
		});
	}

	@SubscribeEvent
	public static void onSetScreen(ScreenEvent.Opening event) {
		if (enabled && isLoadingScreen(event.getNewScreen())) {
			alpha = 1.0f;
			lastAlpha = 1.0f;
		}
	}

	@SubscribeEvent
	public static void onRenderScreen(ScreenEvent.Render.Post event) {
		// Only render over the topmost layer
		if (event.getScreen() == Minecraft.getInstance().screen) {
			float partialTicks = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
			draw(event.getGuiGraphics(), partialTicks);
		}
	}

	@SubscribeEvent
	public static void onClientLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
		enabled = false;
		alpha = 0.0f;
	}

	private static void draw(GuiGraphics graphics, float partialTicks) {
		float frameAlpha = Mth.lerp(partialTicks, lastAlpha, alpha);
		if (frameAlpha > 0.0f) {
			graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), ARGB.color(frameAlpha, CommonColors.BLACK));
		}
	}

	public static void handle(ClientboundFadeToBlackPacket packet, IPayloadContext context) {
		enabled = packet.fadeIn();
		speed = 1.0f / packet.duration();
	}

	private static boolean isLoadingScreen(final @Nullable Screen screen) {
		return screen instanceof ProgressScreen || screen instanceof ReceivingLevelScreen || screen instanceof LevelLoadingScreen;
	}
}
