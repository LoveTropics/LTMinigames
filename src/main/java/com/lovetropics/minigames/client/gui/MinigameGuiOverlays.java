package com.lovetropics.minigames.client.gui;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public class MinigameGuiOverlays {

	private static final Identifier COLADARAL_DAMAGE_OVERLAY = LoveTropics.id("textures/gui/coladaral_damage_outline.png");

	@SubscribeEvent
	public static void registerGuiLayers(RegisterGuiLayersEvent event) {
		event.registerBelow(VanillaGuiLayers.CAMERA_OVERLAYS, LoveTropics.id("coladaral_damage"), MinigameGuiOverlays::renderColadaralDamage);
	}

	private static void renderColadaralDamage(GuiGraphicsExtractor graphics, DeltaTracker tracker) {
		final LocalPlayer player = Minecraft.getInstance().player;

		if (player.hasEffect(EscapeRace.COLADARAL_DAMAGE.getDelegate())) {
			renderTextureOverlay(graphics, COLADARAL_DAMAGE_OVERLAY, 0.9f);
		}
	}

	private static void renderTextureOverlay(GuiGraphicsExtractor guiGraphics, Identifier shaderLocation, float alpha) {
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
