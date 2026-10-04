package org.lovetropics.games.client.game.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.client.LTKeybinds;
import org.lovetropics.games.client.game.bingo.BingoBoardScreen;
import org.lovetropics.games.common.core.game.client_state.instance.BingoBoardClientState;

import java.util.List;
import java.util.Optional;

/// Shows a compact bingo board in the corner of the screen, which can be opened in full with [BingoBoardScreen]
@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public class GameBingoHandler {
	private static final int GAP = 4;
	private static final int TILE_SIZE = 16 + 2 * 2; // 2px padding

	public static final int COMPLETED_COLOR = 0xAA61be29;
	public static final int TILE_COLOR = 0xAA222222;
	public static final int LOCKED_COLOR = 0x66000000;
	public static final int BORDER_COLOR = 0xFFFFFFFF;
	public static final int LOCKED_BORDER_COLOR = 0xFF555555;
	public static final int LOCKED_TEXT_COLOR = 0xFF888888;

	@Nullable
	private static BingoBoardClientState board;

	static final ClientGameStateHandler<BingoBoardClientState> HANDLER = new ClientGameStateHandler<>() {
		@Override
		public void accept(BingoBoardClientState state) {
			board = state;
		}

		@Override
		public void disable(BingoBoardClientState state) {
			board = null;
		}
	};

	public static @Nullable BingoBoardClientState board() {
		return board;
	}

	@SubscribeEvent
	static void onClientTick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		while (LTKeybinds.EXPAND_BINGO_BOARD.consumeClick()) {
			if (board != null && minecraft.gui.screen() == null) {
				minecraft.setScreenAndShow(new BingoBoardScreen());
			}
		}
	}

	@SubscribeEvent
	static void registerLayers(RegisterGuiLayersEvent event) {
		event.registerAboveAll(Identifier.fromNamespaceAndPath(LoveTropics.ID, "bingo_board"), (guiGraphics, deltaTracker) -> {
			BingoBoardClientState board = GameBingoHandler.board;
			if (board == null || board.rows() <= 0 || board.columns() <= 0 || Minecraft.getInstance().gui.screen() instanceof BingoBoardScreen) {
				return;
			}
			int rows = board.rows();
			int columns = board.columns();
			List<Optional<BingoBoardClientState.Tile>> tiles = board.tiles();

			Font font = Minecraft.getInstance().font;

			int boardX = 20, boardY = 20;
			int boardW = columns * TILE_SIZE + (columns - 1) * GAP;
			int boardH = rows * TILE_SIZE + (rows - 1) * GAP;

			// Full board background
			guiGraphics.fill(boardX - 3, boardY - 3, boardX + boardW + 3, boardY + boardH + 3, 0x66000000);

			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < columns; c++) {
					int x = boardX + c * (TILE_SIZE + GAP);
					int y = boardY + r * (TILE_SIZE + GAP);

					int index = r * columns + c;
					Optional<BingoBoardClientState.Tile> optionalTile = tiles.get(index);
					if (optionalTile.isEmpty()) {
						if (board.getLockedSlot(index).isPresent()) {
							drawLockedBackground(guiGraphics, x, y, TILE_SIZE, TILE_SIZE);
							drawLockedIcon(guiGraphics, font, x, y, TILE_SIZE, TILE_SIZE);
						}
						continue;
					}

					BingoBoardClientState.Tile tile = optionalTile.orElseThrow();
					drawTileBackground(guiGraphics, tile, x, y, TILE_SIZE, TILE_SIZE);

					int iconX = x + (TILE_SIZE - 16) / 2;
					int iconY = y + (TILE_SIZE - 16) / 2;
					if (!tile.icon().isEmpty()) {
						guiGraphics.item(tile.icon(), iconX, iconY);
						guiGraphics.itemDecorations(font, tile.icon(), iconX, iconY);
					}

					// How many players beat us to it, in the corner away from the item count
					if (!tile.completed() && tile.completions() > 0) {
						Matrix3x2fStack pose = guiGraphics.pose();
						pose.pushMatrix();
						pose.translate(x + 2, y + 2);
						pose.scale(0.5f, 0.5f);
						guiGraphics.text(font, String.valueOf(tile.completions()), 0, 0, 0xFFFFAA00, true);
						pose.popMatrix();
					}
				}
			}
		});
	}

	public static void drawTileBackground(GuiGraphicsExtractor guiGraphics, BingoBoardClientState.Tile tile, int x, int y, int w, int h) {
		guiGraphics.fill(x, y, x + w, y + h, tile.completed() ? COMPLETED_COLOR : TILE_COLOR);
		drawBorder(guiGraphics, x, y, w, h, BORDER_COLOR);
	}

	public static void drawLockedBackground(GuiGraphicsExtractor guiGraphics, int x, int y, int w, int h) {
		guiGraphics.fill(x, y, x + w, y + h, LOCKED_COLOR);
		drawBorder(guiGraphics, x, y, w, h, LOCKED_BORDER_COLOR);
	}

	/// Draws a question mark in the middle of the given area, where a locked tile would have its icon
	public static void drawLockedIcon(GuiGraphicsExtractor guiGraphics, Font font, int x, int y, int w, int h) {
		String questionMark = "?";
		guiGraphics.text(font, questionMark, x + (w - font.width(questionMark) + 1) / 2, y + (h - font.lineHeight) / 2 + 1, LOCKED_TEXT_COLOR, false);
	}

	private static void drawBorder(GuiGraphicsExtractor guiGraphics, int x, int y, int w, int h, int color) {
		guiGraphics.fill(x, y, x + w, y + 1, color);
		guiGraphics.fill(x, y + h - 1, x + w, y + h, color);
		guiGraphics.fill(x, y, x + 1, y + h, color);
		guiGraphics.fill(x + w - 1, y, x + w, y + h, color);
	}
}
