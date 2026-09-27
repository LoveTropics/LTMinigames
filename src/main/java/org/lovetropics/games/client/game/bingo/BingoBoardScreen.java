package org.lovetropics.games.client.game.bingo;

import org.lovetropics.games.client.LTKeybinds;
import org.lovetropics.games.client.game.handler.GameBingoHandler;
import org.lovetropics.games.common.content.bingo.Bingo;
import org.lovetropics.games.common.core.game.client_state.instance.BingoBoardClientState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/// The full bingo board, with tooltips giving the details of each tile
public final class BingoBoardScreen extends Screen {
	private static final int TILE_W = 64;
	private static final int GAP = 4;
	private static final int ICON_SIZE = 20;
	private static final int TOP_PADDING = 3;
	private static final int SIDE_PADDING = 3;
	private static final int ICON_TEXT_GAP = 2;
	private static final int BOTTOM_PADDING = 3;
	private static final int SCREEN_MARGIN = 10;
	private static final int TOOLTIP_WIDTH = 200;

	public BingoBoardScreen() {
		super(Bingo.BOARD_TITLE);
	}

	@Override
	public void tick() {
		super.tick();
		// The game is over, or we left it
		if (GameBingoHandler.board() == null) {
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
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);

		BingoBoardClientState board = GameBingoHandler.board();
		if (board == null || board.rows() <= 0 || board.columns() <= 0) {
			return;
		}
		int rows = board.rows();
		int columns = board.columns();
		List<Optional<BingoBoardClientState.Tile>> tiles = board.tiles();

		int textAreaW = TILE_W - SIDE_PADDING * 2;
		int lineHeight = font.lineHeight;

		// All tiles are as tall as the one with the longest title
		int maxLines = tiles.stream()
				.flatMap(Optional::stream)
				.map(t -> font.split(t.title(), textAreaW).size())
				.max(Comparator.naturalOrder()).orElse(1);
		int tileH = TOP_PADDING + ICON_SIZE + ICON_TEXT_GAP + maxLines * lineHeight + BOTTOM_PADDING;

		int boardW = columns * TILE_W + (columns - 1) * GAP;
		int boardH = rows * tileH + (rows - 1) * GAP;

		Component header = createHeader(board);
		int headerH = lineHeight * 2 + GAP * 2;
		int contentW = Math.max(boardW, font.width(header));
		int contentH = headerH + boardH;

		// Shrink the board on small screens rather than cutting it off
		float scale = Math.min(1.0f, Math.min((float) (width - SCREEN_MARGIN * 2) / contentW, (float) (height - SCREEN_MARGIN * 2) / contentH));
		float originX = (width - contentW * scale) / 2.0f;
		float originY = (height - contentH * scale) / 2.0f;
		int localMouseX = (int) Math.floor((mouseX - originX) / scale);
		int localMouseY = (int) Math.floor((mouseY - originY) / scale);

		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(originX, originY);
		pose.scale(scale, scale);

		graphics.centeredText(font, title, contentW / 2, 0, 0xFFFFFFFF);
		graphics.centeredText(font, header, contentW / 2, lineHeight + GAP, 0xFFFFFFFF);

		int boardX = (contentW - boardW) / 2;
		int boardY = headerH;
		graphics.fill(boardX - 3, boardY - 3, boardX + boardW + 3, boardY + boardH + 3, 0x66000000);

		@Nullable List<FormattedCharSequence> tooltip = null;

		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < columns; c++) {
				int x = boardX + c * (TILE_W + GAP);
				int y = boardY + r * (tileH + GAP);
				boolean hovered = localMouseX >= x && localMouseX < x + TILE_W && localMouseY >= y && localMouseY < y + tileH;

				int index = r * columns + c;
				Optional<BingoBoardClientState.Tile> optionalTile = tiles.get(index);
				if (optionalTile.isEmpty()) {
					Optional<BingoBoardClientState.LockedSlot> lockedSlot = board.getLockedSlot(index);
					if (lockedSlot.isPresent()) {
						drawLockedTile(graphics, x, y, tileH, maxLines);
						if (hovered) {
							tooltip = createLockedTooltip(board, lockedSlot.get());
						}
					}
					continue;
				}

				BingoBoardClientState.Tile tile = optionalTile.orElseThrow();
				drawTile(graphics, tile, x, y, tileH, maxLines);
				if (hovered) {
					tooltip = createTooltip(tile);
				}
			}
		}

		pose.popMatrix();

		if (tooltip != null) {
			graphics.setTooltipForNextFrame(font, tooltip, mouseX, mouseY);
		}
	}

	private void drawTile(GuiGraphicsExtractor graphics, BingoBoardClientState.Tile tile, int x, int y, int tileH, int maxLines) {
		GameBingoHandler.drawTileBackground(graphics, tile, x, y, TILE_W, tileH);

		int iconX = x + (TILE_W - ICON_SIZE) / 2;
		int iconY = y + TOP_PADDING;

		if (!tile.icon().isEmpty()) {
			Matrix3x2fStack pose = graphics.pose();
			pose.pushMatrix();
			float scale = ICON_SIZE / 16.0f;
			pose.translate(iconX, iconY);
			pose.scale(scale, scale);
			graphics.item(tile.icon(), 0, 0);
			pose.popMatrix();

			graphics.itemDecorations(font, tile.icon(), iconX, iconY);
		}

		int textAreaW = TILE_W - SIDE_PADDING * 2;
		int textAreaY = iconY + ICON_SIZE + ICON_TEXT_GAP;
		List<FormattedCharSequence> lines = font.split(tile.title(), textAreaW);
		int offset = (maxLines - lines.size()) * font.lineHeight / 2;
		for (int i = 0; i < lines.size(); i++) {
			FormattedCharSequence line = lines.get(i);
			graphics.text(font, line, x + (TILE_W - font.width(line)) / 2, textAreaY + i * font.lineHeight + offset, 0xFFFFFFFF, false);
		}
	}

	private void drawLockedTile(GuiGraphicsExtractor graphics, int x, int y, int tileH, int maxLines) {
		GameBingoHandler.drawLockedBackground(graphics, x, y, TILE_W, tileH);
		// Keep the question mark where the icon of other tiles is
		GameBingoHandler.drawLockedIcon(graphics, font, x, y + TOP_PADDING, TILE_W, ICON_SIZE);

		int textY = y + TOP_PADDING + ICON_SIZE + ICON_TEXT_GAP + (maxLines - 1) * font.lineHeight / 2;
		graphics.centeredText(font, Bingo.TILE_LOCKED, x + TILE_W / 2, textY, GameBingoHandler.LOCKED_TEXT_COLOR);
	}

	private List<FormattedCharSequence> createTooltip(BingoBoardClientState.Tile tile) {
		List<FormattedCharSequence> lines = new ArrayList<>();
		lines.addAll(font.split(tile.title().copy().withStyle(ChatFormatting.YELLOW), TOOLTIP_WIDTH));
		lines.add(CommonComponents.EMPTY.getVisualOrderText());
		Component status = tile.completed()
				? Bingo.TILE_DONE.copy().withStyle(ChatFormatting.GREEN)
				: Bingo.TILE_REWARD.apply(tile.reward()).withStyle(ChatFormatting.GOLD);
		lines.add(status.getVisualOrderText());
		if (tile.completions() > 0) {
			lines.add(Bingo.TILE_COMPLETIONS.apply(tile.completions()).withStyle(ChatFormatting.GRAY).getVisualOrderText());
		}
		return lines;
	}

	private List<FormattedCharSequence> createLockedTooltip(BingoBoardClientState board, BingoBoardClientState.LockedSlot slot) {
		List<FormattedCharSequence> lines = new ArrayList<>();
		lines.add(Bingo.TILE_LOCKED.copy().withStyle(ChatFormatting.GRAY).getVisualOrderText());
		slot.clue().ifPresent(clue -> lines.addAll(font.split(clue.copy().withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC), TOOLTIP_WIDTH)));
		board.unlockHint().ifPresent(hint -> {
			lines.add(CommonComponents.EMPTY.getVisualOrderText());
			lines.addAll(font.split(hint.copy().withStyle(ChatFormatting.GOLD), TOOLTIP_WIDTH));
		});
		return lines;
	}

	private static Component createHeader(BingoBoardClientState board) {
		int unlocked = (int) board.tiles().stream().filter(Optional::isPresent).count();
		int completed = (int) board.tiles().stream().flatMap(Optional::stream).filter(BingoBoardClientState.Tile::completed).count();
		MutableComponent header = Bingo.BOARD_HEADER.apply(board.points(), completed, unlocked).withStyle(ChatFormatting.GOLD);
		if (!board.lockedSlots().isEmpty()) {
			header.append(Component.literal(" - ").withStyle(ChatFormatting.GRAY))
					.append(Bingo.BOARD_LOCKED_COUNT.apply(board.lockedSlots().size()).withStyle(ChatFormatting.GRAY));
		}
		return header;
	}
}
