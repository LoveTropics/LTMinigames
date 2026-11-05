package org.lovetropics.games.client.game.overlord;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.lovetropics.games.client.LTKeybinds;
import org.lovetropics.games.client.game.bingo.BingoStandings;
import org.lovetropics.games.client.game.handler.GameBingoHandler;
import org.lovetropics.games.client.game.shop.ShopScreen;
import org.lovetropics.games.client.screen.ClientPlayerInfo;
import org.lovetropics.games.common.content.shop.ShopTexts;
import org.lovetropics.games.common.content.tropic_rush.TropicRushTexts;
import org.lovetropics.games.common.core.game.client_state.instance.BingoBoardClientState;

import java.util.List;

/// Overlord screen for overseeing games
public final class OverlordScreen extends Screen {
	private static final int SCROLLBAR_WIDTH = 6;
	private static final int SCROLLBAR_HEIGHT = 32;
	private static final Identifier SCROLLER_SPRITE = Identifier.withDefaultNamespace("widget/scroller");

	private static final int ROW_W = 160;
	private static final int FACE_SIZE = 10;
	private static final int PADDING = 3;
	private static final int GAP = 4;
	private static final int SCREEN_MARGIN = 20;

	private static final int ROW_COLOR = 0xAA222222;
	private static final int BORDER_COLOR = 0xFF555555;

	private final BingoStandings standings = new BingoStandings(true);
	private int scroll;

	public OverlordScreen() {
		super(TropicRushTexts.OVERLORD);
		standings.tick(GameBingoHandler.board());
	}

	public static boolean isOverlord(BingoBoardClientState board) {
		return board.overlord();
	}

	@Override
	public void tick() {
		super.tick();
		BingoBoardClientState board = GameBingoHandler.board();
		if (board == null || !isOverlord(board)) {
			onClose();
			return;
		}
		standings.tick(board);
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
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		scroll -= (int) Math.round(scrollY * 10.0);
		return true;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);

		List<BingoBoardClientState.PlayerProgress> players = standings.players();
		int lineHeight = font.lineHeight;
		int headerH = lineHeight + GAP * 2;
		if (players.isEmpty()) {
			int y = (height - headerH - lineHeight) / 2;
			graphics.centeredText(font, title, width / 2, y, 0xFFFFFFFF);
			graphics.centeredText(font, TropicRushTexts.OVERLORD_NO_PLAYERS.copy().withStyle(ChatFormatting.GRAY), width / 2, y + headerH, 0xFFFFFFFF);
			return;
		}

		int rowH = Math.max(lineHeight, FACE_SIZE) + PADDING * 2;
		int fullH = players.size() * rowH + (players.size() - 1) * GAP;
		int listH = Math.min(fullH, height - SCREEN_MARGIN * 2 - headerH);
		int maxScroll = Math.max(fullH - listH, 0);
		scroll = Mth.clamp(scroll, 0, maxScroll);

		int listX = (width - ROW_W) / 2;
		int listY = (height - listH + headerH) / 2;
		graphics.centeredText(font, title, width / 2, listY - headerH, 0xFFFFFFFF);

		if (maxScroll > 0) {
			int offset = scroll * (listH - SCROLLBAR_HEIGHT) / maxScroll;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_SPRITE, listX + ROW_W + PADDING, listY + offset, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT);
		}

		graphics.enableScissor(listX, listY, listX + ROW_W, listY + listH);
		for (int rank = 0; rank < players.size(); rank++) {
			drawRow(graphics, rank, players.get(rank), listX, listY + rank * (rowH + GAP) - scroll, rowH);
		}
		graphics.disableScissor();
	}

	// TOOD: temporary plz remove when the screen has more info
	private void drawRow(GuiGraphicsExtractor graphics, int rank, BingoBoardClientState.PlayerProgress player, int x, int y, int rowH) {
		ShopScreen.drawFrame(graphics, x, y, ROW_W, rowH, ROW_COLOR, BORDER_COLOR);

		int textY = y + (rowH - font.lineHeight) / 2 + 1;
		int faceX = x + PADDING + 2;
		PlayerFaceExtractor.extractRenderState(graphics, ClientPlayerInfo.getSkin(player.id()), faceX, y + (rowH - FACE_SIZE) / 2, FACE_SIZE);

		Component points = ShopTexts.POINTS.apply(player.points()).withStyle(ChatFormatting.GOLD);
		int pointsX = x + ROW_W - PADDING - 2 - font.width(points);
		graphics.text(font, points, pointsX, textY, 0xFFFFFFFF, false);

		Component place = Component.literal("#" + (rank + 1) + " ").withStyle(ChatFormatting.GRAY);
		int placeX = faceX + FACE_SIZE + 4;
		graphics.text(font, place, placeX, textY, 0xFFFFFFFF, false);

		int nameX = placeX + font.width(place);
		String name = font.plainSubstrByWidth(player.name(), pointsX - 4 - nameX);
		graphics.text(font, name, nameX, textY, 0xFFFFFFFF, false);
	}
}
