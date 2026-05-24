package com.lovetropics.minigames.client.game.handler;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.game.ClientGameStateManager;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.lovetropics.minigames.common.core.game.client_state.instance.SidebarClientState;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.List;

public final class GameSidebarRenderer {

	private static final int PADDING = 2;
	private static final int MARGIN = 1;

	public static void registerOverlays(RegisterGuiLayersEvent event) {
		event.registerBelow(VanillaGuiLayers.CONTEXTUAL_INFO_BAR_BACKGROUND, LoveTropics.location("minigame_sidebar"), (graphics, deltaTracker) -> {
			if (Minecraft.getInstance().options.hideGui) {
				return;
			}
			SidebarClientState sidebar = ClientGameStateManager.getOrNull(GameClientStateTypes.SIDEBAR);
			if (sidebar != null) {
				renderSidebar(graphics, sidebar);
			}
		});
	}

	private static void renderSidebar(GuiGraphicsExtractor graphics, SidebarClientState sidebar) {
		Component title = sidebar.title();
		List<Component> lines = sidebar.lines();

		Minecraft minecraft = Minecraft.getInstance();
		Font font = minecraft.font;
		Window window = minecraft.getWindow();
		Options options = minecraft.options;
		int screenWidth = window.getGuiScaledWidth();
		int screenHeight = window.getGuiScaledHeight();

		int width = font.width(title);
		for (Component line : lines) {
			width = Math.max(width, font.width(line));
		}

		int right = screenWidth - MARGIN;
		int left = right - width - PADDING;
		int height = (lines.size() + 1) * font.lineHeight + PADDING * 2;
		int top = (screenHeight - height) / 2;
		int bottom = top + height;

		int headerBottom = top + font.lineHeight + PADDING;
		graphics.fill(left, top, right, headerBottom, options.getBackgroundColor(0.4f));
		graphics.fill(left, headerBottom, right, bottom, options.getBackgroundColor(0.3f));

		int textLeft = left + PADDING;
		graphics.text(font, title, textLeft, top + PADDING, CommonColors.WHITE);

		int y = headerBottom + 1;
		for (Component line : lines) {
			graphics.text(font, line, textLeft, y, CommonColors.WHITE);
			y += font.lineHeight;
		}
	}
}
