package org.lovetropics.games.client.game.modifier;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2fStack;
import org.lovetropics.games.client.LTKeybinds;
import org.lovetropics.games.client.game.handler.GameModifiersHandler;
import org.lovetropics.games.common.content.mcsr.McsrTexts;
import org.lovetropics.games.common.core.game.client_state.instance.ModifiersClientState;

import java.util.List;

/// Lists the modifiers of the game and how strong they are, in the style of a container. Players only see a riddle for
/// the modifiers they haven't figured out yet.
public final class ModifiersScreen extends Screen {
	private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
	private static final Identifier BAR_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("boss_bar/purple_background");
	private static final Identifier BAR_PROGRESS_SPRITE = Identifier.withDefaultNamespace("boss_bar/purple_progress");

	private static final int PANEL_PADDING = 7;
	private static final int SLOT_SIZE = 18;
	private static final int SLOT_TEXT_GAP = 4;
	private static final int BAR_W = 182;
	private static final int BAR_H = 5;
	private static final int BAR_GAP = 2;
	private static final int ROW_GAP = 5;
	private static final int CONTENT_W = SLOT_SIZE + SLOT_TEXT_GAP + BAR_W;
	private static final int PANEL_W = CONTENT_W + PANEL_PADDING * 2;
	private static final int SCREEN_MARGIN = 10;

	// The colors of a vanilla container
	private static final int PANEL_COLOR = 0xFFC6C6C6;
	private static final int PANEL_OUTLINE_COLOR = 0xFF000000;
	private static final int PANEL_LIGHT_COLOR = 0xFFFFFFFF;
	private static final int PANEL_SHADOW_COLOR = 0xFF555555;
	private static final int SEPARATOR_COLOR = 0xFF8B8B8B;
	private static final int TEXT_COLOR = 0xFF404040;
	private static final int DESCRIPTION_COLOR = 0xFF555555;
	private static final int RIDDLE_COLOR = 0xFF7A3D9A;
	private static final int UNKNOWN_COLOR = 0xFF8B8B8B;

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
		int titleH = lineHeight + 4;

		int textW = CONTENT_W - SLOT_SIZE - SLOT_TEXT_GAP;
		List<List<FormattedCharSequence>> details = entries.stream().map(entry -> font.split(getDetails(entry), textW)).toList();
		int contentH = titleH;
		if (entries.isEmpty()) {
			contentH += lineHeight;
		}
		for (int i = 0; i < details.size(); i++) {
			contentH += (i > 0 ? ROW_GAP : 0) + getRowHeight(details.get(i));
		}
		int panelH = contentH + PANEL_PADDING * 2 - 1;

		// Shrink the panel on small screens rather than cutting it off
		float scale = Math.min(1.0f, Math.min((float) (width - SCREEN_MARGIN * 2) / PANEL_W, (float) (height - SCREEN_MARGIN * 2) / panelH));
		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate((width - PANEL_W * scale) / 2.0f, (height - panelH * scale) / 2.0f);
		pose.scale(scale, scale);

		drawPanel(graphics, 0, 0, PANEL_W, panelH);
		graphics.text(font, title, PANEL_PADDING + 1, PANEL_PADDING - 1, TEXT_COLOR, false);

		int y = PANEL_PADDING - 1 + titleH;
		if (entries.isEmpty()) {
			graphics.text(font, McsrTexts.RULES_EMPTY, PANEL_PADDING + 1, y, DESCRIPTION_COLOR, false);
		}
		for (int i = 0; i < entries.size(); i++) {
			if (i > 0) {
				// Engraved, like the separators of vanilla screens
				int separatorY = y + ROW_GAP / 2 - 1;
				graphics.fill(PANEL_PADDING, separatorY, PANEL_PADDING + CONTENT_W, separatorY + 1, SEPARATOR_COLOR);
				graphics.fill(PANEL_PADDING, separatorY + 1, PANEL_PADDING + CONTENT_W, separatorY + 2, PANEL_LIGHT_COLOR);
				y += ROW_GAP;
			}
			drawRow(graphics, entries.get(i), details.get(i), PANEL_PADDING, y);
			y += getRowHeight(details.get(i));
		}

		pose.popMatrix();
	}

	private int getRowHeight(List<FormattedCharSequence> details) {
		int textH = font.lineHeight * (1 + details.size()) + BAR_GAP + BAR_H;
		return Math.max(SLOT_SIZE, textH);
	}

	private void drawRow(GuiGraphicsExtractor graphics, ModifiersClientState.Entry entry, List<FormattedCharSequence> details, int x, int y) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x, y, SLOT_SIZE, SLOT_SIZE);
		if (entry.revealed() && !entry.icon().isEmpty()) {
			graphics.item(entry.icon(), x + 1, y + 1);
		} else {
			graphics.centeredText(font, Component.literal("?"), x + SLOT_SIZE / 2, y + (SLOT_SIZE - font.lineHeight) / 2 + 1, RIDDLE_COLOR);
		}

		int textX = x + SLOT_SIZE + SLOT_TEXT_GAP;
		int textY = y + 1;
		if (entry.name().isPresent()) {
			graphics.text(font, entry.name().get(), textX, textY, TEXT_COLOR, false);
		} else {
			graphics.text(font, McsrTexts.RULE_UNKNOWN, textX, textY, UNKNOWN_COLOR, false);
		}
		Component level = McsrTexts.RULE_LEVEL.apply(entry.level(), entry.maxLevel());
		graphics.text(font, level, x + CONTENT_W - font.width(level), textY, TEXT_COLOR, false);

		int detailsColor = entry.revealed() ? DESCRIPTION_COLOR : RIDDLE_COLOR;
		for (int i = 0; i < details.size(); i++) {
			graphics.text(font, details.get(i), textX, textY + font.lineHeight * (i + 1), detailsColor, false);
		}

		// How strong the modifier is, even for players who don't know what it is yet
		int barY = textY + font.lineHeight * (1 + details.size()) + BAR_GAP;
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BAR_BACKGROUND_SPRITE, textX, barY, BAR_W, BAR_H);
		int filledW = entry.maxLevel() > 0 ? BAR_W * Math.clamp(entry.level(), 0, entry.maxLevel()) / entry.maxLevel() : 0;
		if (filledW > 0) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BAR_PROGRESS_SPRITE, BAR_W, BAR_H, 0, 0, textX, barY, filledW, BAR_H);
		}
	}

	private static Component getDetails(ModifiersClientState.Entry entry) {
		return entry.description()
				.orElseGet(() -> entry.riddle().copy().withStyle(ChatFormatting.ITALIC));
	}

	/// Draws a panel like the background of a vanilla container, with an outline and bevelled edges
	private static void drawPanel(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
		// The outline leaves the corners out, which rounds them off
		graphics.fill(x + 1, y, x + w - 1, y + 1, PANEL_OUTLINE_COLOR);
		graphics.fill(x + 1, y + h - 1, x + w - 1, y + h, PANEL_OUTLINE_COLOR);
		graphics.fill(x, y + 1, x + 1, y + h - 1, PANEL_OUTLINE_COLOR);
		graphics.fill(x + w - 1, y + 1, x + w, y + h - 1, PANEL_OUTLINE_COLOR);

		graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL_COLOR);
		graphics.fill(x + 1, y + 1, x + w - 3, y + 3, PANEL_LIGHT_COLOR);
		graphics.fill(x + 1, y + 1, x + 3, y + h - 3, PANEL_LIGHT_COLOR);
		graphics.fill(x + 3, y + h - 3, x + w - 1, y + h - 1, PANEL_SHADOW_COLOR);
		graphics.fill(x + w - 3, y + 3, x + w - 1, y + h - 1, PANEL_SHADOW_COLOR);
	}
}
