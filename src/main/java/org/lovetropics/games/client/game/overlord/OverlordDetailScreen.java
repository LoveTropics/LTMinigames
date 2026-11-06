package org.lovetropics.games.client.game.overlord;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.Mth;
import org.lovetropics.games.client.LTKeybinds;

public final class OverlordDetailScreen extends Screen {
	private final OverlordWidget widget;
	private final Screen parent;
	private int scroll;
	private int maxScroll;

	OverlordDetailScreen(OverlordWidget widget, Screen parent) {
		super(widget.title);
		this.widget = widget;
		this.parent = parent;
	}

	@Override
	public void tick() {
		super.tick();
		if (OverlordScreen.Data.get() == null) {
			minecraft.gui.setScreen(null);}
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (LTKeybinds.EXPAND_BINGO_BOARD.matches(event)) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, maxScroll);
		return true;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		OverlordScreen.Data data = OverlordScreen.Data.get();
		if (data == null) {
			return;
		}
		int margin = OverlordScreen.SCREEN_MARGIN;
		scroll = Math.min(scroll, maxScroll);
		maxScroll = OverlordScreen.drawWidget(graphics, font, widget, data, margin, margin, width - margin * 2, height - margin * 2, false, scroll);
	}
}
