package com.lovetropics.minigames.common.content.escape_race.client;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.game.ClientGameStateManager;
import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.CommonColors;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public class EscapeRaceBucksRenderer {
	private static final int PADDING = 2;
	private static final int ITEM_SIZE = 16;

	public static void registerOverlays(RegisterGuiLayersEvent event) {
		event.registerAbove(VanillaGuiLayers.EXPERIENCE_LEVEL, LoveTropics.location("escape_race_bucks"), (graphics, deltaTracker) -> {
			if (Minecraft.getInstance().options.hideGui) {
				return;
			}
			EscapeRaceClientBucksState escapeRaceClientBucksState = ClientGameStateManager.getOrNull(EscapeRace.BREAK_BUCK_STATE);
			if (escapeRaceClientBucksState != null) {
				renderOverlay(graphics, escapeRaceClientBucksState);
			}
		});
	}

	private static void renderOverlay(GuiGraphics graphics, EscapeRaceClientBucksState selfState) {
		Font font = Minecraft.getInstance().font;

		final int left = PADDING;
		final int top = PADDING;

		int x = (graphics.guiWidth() / 2) - 8;
		int y = graphics.guiHeight() - 40;

		graphics.renderItem(EscapeRace.BREAK_BUCK.asStack(), x, y);

		String currency = String.valueOf(selfState.amount());

		graphics.drawString(
				font, currency,
				x + (ITEM_SIZE / 2) - 2,
				y + ((18 - font.lineHeight) / 2) + 8,
				CommonColors.WHITE
		);
	}
}
