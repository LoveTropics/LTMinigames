package org.lovetropics.games.client.game.modifier;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2fStack;
import org.lovetropics.games.client.LTKeybinds;
import org.lovetropics.games.client.game.handler.GameModifiersHandler;
import org.lovetropics.games.common.content.mcsr.McsrTexts;
import org.lovetropics.games.common.core.game.client_state.instance.ModifiersClientState;

import java.util.List;

/// Lists the modifiers of the game and how strong they are. Players only see a riddle for the modifiers they haven't
/// figured out yet.
public final class ModifiersScreen extends Screen {
	private static final int ROW_W = 240;
	private static final int GAP = 4;
	private static final int ICON_SIZE = 16;
	private static final int PADDING = 4;
	private static final int BAR_H = 3;
	private static final int SCREEN_MARGIN = 10;

	private static final int ROW_COLOR = 0xAA222222;
	private static final int BORDER_COLOR = 0xFF555555;
	private static final int REVEALED_BORDER_COLOR = 0xFFAA55FF;
	private static final int UNKNOWN_ICON_COLOR = 0x66000000;
	private static final int BAR_BACKGROUND_COLOR = 0xFF333333;
	private static final int BAR_COLOR = 0xFFAA55FF;

	public ModifiersScreen() {
		super(McsrTexts.RULES);
	}

	@Override
	public void tick() {
		super.tick();
		// The game is over, or we left it
		if (GameModifiersHandler.modifiers() == null) {
			onClose();
		}
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (LTKeybinds.OPEN_RULES.matches(event)) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);

		ModifiersClientState modifiers = GameModifiersHandler.modifiers();
		if (modifiers == null) {
			return;
		}
		List<ModifiersClientState.Entry> entries = modifiers.entries();
		int lineHeight = font.lineHeight;
		int headerH = lineHeight + GAP * 2;
		if (entries.isEmpty()) {
			graphics.centeredText(font, title, width / 2, height / 2 - headerH, 0xFFFFFFFF);
			graphics.centeredText(font, McsrTexts.RULES_EMPTY.copy().withStyle(ChatFormatting.GRAY), width / 2, height / 2, 0xFFFFFFFF);
			return;
		}

		int textW = ROW_W - PADDING * 3 - ICON_SIZE;
		List<List<FormattedCharSequence>> details = entries.stream().map(entry -> font.split(getDetails(entry), textW)).toList();
		int contentH = headerH;
		for (List<FormattedCharSequence> lines : details) {
			contentH += getRowHeight(lines) + GAP;
		}
		contentH -= GAP;

		// Shrink the list on small screens rather than cutting it off
		float scale = Math.min(1.0f, Math.min((float) (width - SCREEN_MARGIN * 2) / ROW_W, (float) (height - SCREEN_MARGIN * 2) / contentH));
		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate((width - ROW_W * scale) / 2.0f, (height - contentH * scale) / 2.0f);
		pose.scale(scale, scale);

		graphics.centeredText(font, title, ROW_W / 2, 0, 0xFFFFFFFF);
		int y = headerH;
		for (int i = 0; i < entries.size(); i++) {
			int rowH = getRowHeight(details.get(i));
			drawRow(graphics, entries.get(i), details.get(i), y, rowH);
			y += rowH + GAP;
		}

		pose.popMatrix();
	}

	private int getRowHeight(List<FormattedCharSequence> details) {
		int textH = font.lineHeight * (1 + details.size());
		return PADDING + Math.max(ICON_SIZE, textH) + GAP + BAR_H + PADDING;
	}

	private void drawRow(GuiGraphicsExtractor graphics, ModifiersClientState.Entry entry, List<FormattedCharSequence> details, int y, int rowH) {
		drawFrame(graphics, 0, y, ROW_W, rowH, ROW_COLOR, entry.revealed() ? REVEALED_BORDER_COLOR : BORDER_COLOR);

		int iconX = PADDING;
		int iconY = y + PADDING;
		if (entry.revealed() && !entry.icon().isEmpty()) {
			graphics.item(entry.icon(), iconX, iconY);
		} else {
			graphics.fill(iconX, iconY, iconX + ICON_SIZE, iconY + ICON_SIZE, UNKNOWN_ICON_COLOR);
			graphics.centeredText(font, Component.literal("?").withStyle(ChatFormatting.LIGHT_PURPLE), iconX + ICON_SIZE / 2, iconY + (ICON_SIZE - font.lineHeight) / 2 + 1, 0xFFFFFFFF);
		}

		int textX = iconX + ICON_SIZE + PADDING;
		Component name = entry.name()
				.map(n -> n.copy().withStyle(ChatFormatting.AQUA))
				.orElseGet(() -> McsrTexts.RULE_UNKNOWN.copy().withStyle(ChatFormatting.GRAY));
		graphics.text(font, name, textX, iconY, 0xFFFFFFFF, false);
		Component level = McsrTexts.RULE_LEVEL.apply(entry.level(), entry.maxLevel()).withStyle(ChatFormatting.GOLD);
		graphics.text(font, level, ROW_W - PADDING - font.width(level), iconY, 0xFFFFFFFF, false);

		for (int i = 0; i < details.size(); i++) {
			graphics.text(font, details.get(i), textX, iconY + font.lineHeight * (i + 1), 0xFFFFFFFF, false);
		}

		// How strong the modifier is, even for players who don't know what it is yet
		int barY = y + rowH - PADDING - BAR_H;
		int barW = ROW_W - PADDING * 2;
		int filledW = entry.maxLevel() > 0 ? barW * Math.clamp(entry.level(), 0, entry.maxLevel()) / entry.maxLevel() : 0;
		graphics.fill(PADDING, barY, PADDING + barW, barY + BAR_H, BAR_BACKGROUND_COLOR);
		graphics.fill(PADDING, barY, PADDING + filledW, barY + BAR_H, BAR_COLOR);
	}

	private static Component getDetails(ModifiersClientState.Entry entry) {
		return entry.description()
				.map(description -> description.copy().withStyle(ChatFormatting.GRAY))
				.orElseGet(() -> entry.riddle().copy().withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
	}

	private static void drawFrame(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int background, int border) {
		graphics.fill(x, y, x + w, y + h, background);
		graphics.fill(x, y, x + w, y + 1, border);
		graphics.fill(x, y + h - 1, x + w, y + h, border);
		graphics.fill(x, y, x + 1, y + h, border);
		graphics.fill(x + w - 1, y, x + w, y + h, border);
	}
}
