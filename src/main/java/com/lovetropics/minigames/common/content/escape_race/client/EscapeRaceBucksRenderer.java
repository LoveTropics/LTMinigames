package com.lovetropics.minigames.common.content.escape_race.client;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.game.ClientGameStateManager;
import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CommonColors;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.function.UnaryOperator;

@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public class EscapeRaceBucksRenderer {
	private static final int PADDING = 2;
	private static final int ITEM_SIZE = 16;
	private static final ResourceLocation HOTBAR_SPRITE = LoveTropics.location("break_bucks_holder");

	private static final UnaryOperator<GuiLayer> HIDE_IN_ESCAPE_RACE = (layer) -> ((guiGraphics, deltaTracker) -> {
		EscapeRaceClientBucksState escapeRaceClientBucksState = ClientGameStateManager.getOrNull(EscapeRace.BREAK_BUCK_STATE);
		if (escapeRaceClientBucksState == null) {
			layer.render(guiGraphics, deltaTracker);
		}
	});

	public static void registerOverlays(RegisterGuiLayersEvent event) {
		event.wrapLayer(VanillaGuiLayers.CONTEXTUAL_INFO_BAR_BACKGROUND, (layer) -> ((guiGraphics, deltaTracker) -> {
			EscapeRaceClientBucksState escapeRaceClientBucksState = ClientGameStateManager.getOrNull(EscapeRace.BREAK_BUCK_STATE);
			if (escapeRaceClientBucksState == null) {
				layer.render(guiGraphics, deltaTracker);
			} else {
				renderOverlay(guiGraphics, escapeRaceClientBucksState);
			}
		}));
		event.wrapLayer(VanillaGuiLayers.PLAYER_HEALTH, HIDE_IN_ESCAPE_RACE);
		event.wrapLayer(VanillaGuiLayers.FOOD_LEVEL, HIDE_IN_ESCAPE_RACE);
		event.wrapLayer(VanillaGuiLayers.ARMOR_LEVEL, HIDE_IN_ESCAPE_RACE);
	}

	private static void renderOverlay(GuiGraphics graphics, EscapeRaceClientBucksState selfState) {
		Font font = Minecraft.getInstance().font;

		final int left = PADDING;
		final int top = PADDING;

		int x = (graphics.guiWidth() / 2) - 8;
		int y = graphics.guiHeight() - 40;

		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, x - 21, graphics.guiHeight() - 30, 60, 9);
		graphics.pose().pushMatrix();
//		graphics.pose().translate(x , y + 12.5f);
//		graphics.pose().scale(0.4f,0.4f);
		graphics.renderItem(EscapeRace.BREAK_BUCK.asStack(), x, y - 5);
		graphics.pose().popMatrix();

		String currency = String.format("%04d", selfState.amount());

		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y + 12.5f);
		graphics.pose().scale(0.7f,0.7f);
		graphics.drawString(
				font, currency,
				0,
				0,
				CommonColors.WHITE
		);
		graphics.pose().popMatrix();
	}
}
