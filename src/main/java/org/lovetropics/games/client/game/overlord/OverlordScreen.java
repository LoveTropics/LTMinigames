package org.lovetropics.games.client.game.overlord;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.CommonColors;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.client.LTKeybinds;
import org.lovetropics.games.client.game.bingo.BingoStandings;
import org.lovetropics.games.client.game.handler.GameBingoHandler;
import org.lovetropics.games.client.game.handler.GameShopHandler;
import org.lovetropics.games.client.game.shop.ShopScreen;
import org.lovetropics.games.common.content.tropic_rush.TropicRushTexts;
import org.lovetropics.games.common.core.game.client_state.instance.BingoBoardClientState;
import org.lovetropics.games.common.core.game.client_state.instance.ShopClientState;

import java.util.List;

/// Overlord screen for overseeing games
public final class OverlordScreen extends Screen {
	static final int SCREEN_MARGIN = 20;
	private static final int GAP = 6;
	private static final int PADDING = 4;

	private static final int WIDGET_COLOR = 0xAA222222;
	private static final int BORDER_COLOR = 0xFF555555;
	private static final int HOVERED_BORDER_COLOR = 0xFFFFFFFF;

	private static final OverlordWidget[] WIDGETS = OverlordWidget.values();

	public OverlordScreen() {
		super(TropicRushTexts.OVERLORD);
	}

	public static boolean isOverlord(BingoBoardClientState board) {
		return board.overlord();
	}

	@Override
	public void tick() {
		super.tick();
		// The game is over, we left it, or we aren't an overlord anymore
		if (Data.get() == null) {
			onClose();
		}
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
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int index = getWidgetAt(event.x(), event.y());
		if (index >= 0 && event.input() == 0) {
			minecraft.gui.setScreen(WIDGETS[index].open(this));
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		Data data = Data.get();
		if (data == null) {
			return;
		}
		graphics.centeredText(font, title, width / 2, SCREEN_MARGIN, CommonColors.WHITE);
		int hovered = getWidgetAt(mouseX, mouseY);
		for (int index = 0; index < WIDGETS.length; index++) {
			drawWidget(graphics, font, WIDGETS[index], data, getWidgetX(index), getWidgetY(index), getWidgetW(), getWidgetH(), index == hovered, 0);
		}
	}

	static int drawWidget(GuiGraphicsExtractor graphics, Font font, OverlordWidget widget, Data data, int x, int y, int w, int h, boolean hovered, int scroll) {
		ShopScreen.drawFrame(graphics, x, y, w, h, WIDGET_COLOR, hovered ? HOVERED_BORDER_COLOR : BORDER_COLOR);
		graphics.text(font, widget.title, x + PADDING, y + PADDING, CommonColors.YELLOW, true);

		int contentX = x + PADDING;
		int contentY = y + PADDING + font.lineHeight + GAP;
		int contentW = w - PADDING * 2;
		int contentH = h - PADDING * 2 - font.lineHeight - GAP;
		graphics.enableScissor(contentX, contentY, contentX + contentW, contentY + contentH);
		int maxScroll = widget.draw(graphics, font, data, contentX, contentY, contentW, contentH, scroll);
		graphics.disableScissor();
		return maxScroll;
	}

	private int getWidgetAt(double x, double y) {
		for (int index = 0; index < WIDGETS.length; index++) {
			int widgetX = getWidgetX(index);
			int widgetY = getWidgetY(index);
			if (x >= widgetX && x < widgetX + getWidgetW() && y >= widgetY && y < widgetY + getWidgetH()) {
				return index;
			}
		}
		return -1;
	}

	// The widgets are laid out in two columns below the title
	private int getWidgetX(int index) {
		return SCREEN_MARGIN + (index % 2) * (getWidgetW() + GAP);
	}

	private int getWidgetY(int index) {
		return getGridTop() + (index / 2) * (getWidgetH() + GAP);
	}

	private int getWidgetW() {
		return (width - SCREEN_MARGIN * 2 - GAP) / 2;
	}

	private int getWidgetH() {
		int rows = (WIDGETS.length + 1) / 2;
		return (height - getGridTop() - SCREEN_MARGIN - (rows - 1) * GAP) / rows;
	}

	private int getGridTop() {
		return SCREEN_MARGIN + font.lineHeight + GAP;
	}

	record Data(BingoBoardClientState board, List<BingoBoardClientState.PlayerProgress> players, List<ShopClientState.Sabotage> sabotages) {
		static @Nullable Data get() {
			BingoBoardClientState board = GameBingoHandler.board();
			if (board == null || !isOverlord(board)) {
				return null;
			}
			ShopClientState shop = GameShopHandler.shop();
			List<BingoBoardClientState.PlayerProgress> players = board.players().stream().sorted(BingoStandings.ORDER).toList();
			return new Data(board, players, shop != null ? shop.sabotages() : List.of());
		}
	}
}
