package com.lovetropics.minigames.client.gui;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public class MinigameGuiOverlays {

	private static final ResourceLocation COLADARALL_DAMAGE_OVERLAY = ResourceLocation.withDefaultNamespace("textures/misc/powder_snow_outline.png");

	@SubscribeEvent
	public static void registerGuiLayers(RegisterGuiLayersEvent event) {
		event.registerBelow(VanillaGuiLayers.CAMERA_OVERLAYS, LoveTropics.location("coladarall_damage"), MinigameGuiOverlays::renderColadarallDamage);
	}

	private static void renderColadarallDamage(GuiGraphics graphics, DeltaTracker tracker) {
		final LocalPlayer player = Minecraft.getInstance().player;

		if (player.hasEffect(EscapeRace.COLADARAL_DAMAGE.getDelegate())) {
			renderTextureOverlay(graphics, COLADARALL_DAMAGE_OVERLAY, 1.0f);
		}
	}

	private static void renderTextureOverlay(GuiGraphics guiGraphics, ResourceLocation shaderLocation, float alpha) {
		int i = ARGB.white(alpha);
		guiGraphics.blit(
				RenderPipelines.GUI_TEXTURED,
				shaderLocation,
				0,
				0,
				0.0F,
				0.0F,
				guiGraphics.guiWidth(),
				guiGraphics.guiHeight(),
				guiGraphics.guiWidth(),
				guiGraphics.guiHeight(),
				i
		);
	}
}
