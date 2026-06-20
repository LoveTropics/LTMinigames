package com.lovetropics.minigames.client.toast;

import com.lovetropics.minigames.LoveTropics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public final class NotificationToast implements Toast {
	private static final int ICON_SIZE = 18;
	private static final int TEXT_LEFT = ICON_SIZE + 8;
	private static final int MAX_WIDTH = 160 - TEXT_LEFT;

	private static final int LINE_HEIGHT = 12;

	private final List<FormattedCharSequence> lines;
	private final NotificationStyle style;
	private final Identifier backgroundSprite;

	private final int width;
	private final int height;

	private Toast.Visibility wantedVisibility = Visibility.SHOW;

	public NotificationToast(Component message, NotificationStyle style) {
		Font fontRenderer = Minecraft.getInstance().font;

		List<FormattedCharSequence> lines = new ArrayList<>(2);
		lines.addAll(fontRenderer.split(message, MAX_WIDTH));

		int textWidth = Math.max(lines.stream().mapToInt(fontRenderer::width).max().orElse(MAX_WIDTH), MAX_WIDTH);
		width = TEXT_LEFT + textWidth + 4;
		height = Math.max(lines.size() * LINE_HEIGHT + 8, 26);

		this.lines = lines;
		this.style = style;
		backgroundSprite = LoveTropics.id("toast/" + style.color().getName() + "_" + style.sentiment().getName());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long fullyVisibleForMs) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, backgroundSprite, 0, 0, width, height);

		drawText(graphics);
		drawIcon(graphics);
	}
	
	@Override
	public void update(ToastManager toastManager, long visibilityTime) {
		wantedVisibility = visibilityTime >= style.visibleTimeMs() ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
	}

	@Override
	public Visibility getWantedVisibility() {
		return wantedVisibility;
	}

	private void drawText(GuiGraphicsExtractor graphics) {
		List<FormattedCharSequence> lines = this.lines;
		for (int i = 0; i < lines.size(); i++) {
			FormattedCharSequence line = lines.get(i);
			graphics.text(Minecraft.getInstance().font, line, TEXT_LEFT, 7 + (i * 12), style.color() == NotificationStyle.Color.LIGHT ? CommonColors.BLACK : CommonColors.WHITE, false);
		}
	}

	private void drawIcon(GuiGraphicsExtractor graphics) {
		int y = (height - ICON_SIZE) / 2;

		NotificationIcon icon = style.icon();
		if (icon.item != null) {
			graphics.item(icon.item.create(), 6, y);
		} else if (icon.effect != null) {
			Identifier sprite = Minecraft.getInstance().gui.hud.getMobEffectSprite(icon.effect);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, 5, y, 18, 18);
		}
	}

	@Override
	public int width() {
		return width;
	}

	@Override
	public int height() {
		return height;
	}
}
