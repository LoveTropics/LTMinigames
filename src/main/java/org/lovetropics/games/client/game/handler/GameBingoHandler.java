package org.lovetropics.games.client.game.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.player.LocalPlayer;
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
import org.lovetropics.games.client.game.bingo.BingoStandings;
import org.lovetropics.games.client.screen.ClientPlayerInfo;
import org.lovetropics.games.common.core.game.client_state.instance.BingoBoardClientState;

import java.util.List;
import java.util.Optional;
import java.util.function.IntPredicate;

/// Shows a compact bingo board in the corner of the screen, which can be opened in full with [BingoBoardScreen].
/// Next to it, how far along every player is.
@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public class GameBingoHandler {
	private static final int BOARD_X = 20, BOARD_Y = 20;
	private static final int GAP = 4;
	private static final int TILE_SIZE = 16 + 2 * 2; // 2px padding
	public static final int FACE_SIZE = 8;
	private static final int STANDINGS_GAP = 3;
	private static final int STANDINGS_ROW_HEIGHT = FACE_SIZE + 2;
	private static final int BAR_WIDTH = 40, BAR_HEIGHT = 4;

	public static final int COMPLETED_COLOR = 0xAA61be29;
	public static final int TILE_COLOR = 0xAA222222;
	public static final int LOCKED_COLOR = 0x66000000;
	public static final int BORDER_COLOR = 0xFFFFFFFF;
	public static final int LOCKED_BORDER_COLOR = 0xFF555555;
	public static final int LOCKED_TEXT_COLOR = 0xFF888888;
	public static final int BACKGROUND_COLOR = 0x66000000;
	public static final int POINTS_COLOR = 0xFFFFAA00;
	public static final int OWN_POINTS_COLOR = 0xFFFFFF55;

	@Nullable
	private static BingoBoardClientState board;

	/// Every player of the game, shown next to the board
	private static final BingoStandings STANDINGS = new BingoStandings(true);

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

	/// @return the y position just below the compact board, or `-1` if it isn't shown
	public static int getOverlayBottom() {
		BingoBoardClientState board = GameBingoHandler.board;
		if (board == null || board.rows() <= 0 || board.columns() <= 0) {
			return -1;
		}
		return BOARD_Y + getCompactBoardHeight(board) + 3;
	}

	public static int getCompactBoardWidth(BingoBoardClientState board) {
		return board.columns() * TILE_SIZE + (board.columns() - 1) * GAP;
	}

	public static int getCompactBoardHeight(BingoBoardClientState board) {
		return board.rows() * TILE_SIZE + (board.rows() - 1) * GAP;
	}

	@SubscribeEvent
	static void onClientTick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		STANDINGS.tick(board);
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
			Font font = Minecraft.getInstance().font;
			drawCompactBoard(guiGraphics, font, board, index -> board.tiles().get(index).map(BingoBoardClientState.Tile::completed).orElse(false), true, BOARD_X, BOARD_Y);

			int standingsX = BOARD_X + getCompactBoardWidth(board) + 3 + STANDINGS_GAP + 3;
			drawStandings(guiGraphics, font, board, standingsX, BOARD_Y, deltaTracker.getGameTimeDeltaPartialTick(true));
		});
	}

	/// Draws a board in its compact form, which only shows the icon of each tile
	///
	/// @param completed       whether the player whose board it is has completed the tile at the given index
	/// @param showCompletions whether to show how many players have completed the tiles that aren't completed yet
	public static void drawCompactBoard(GuiGraphicsExtractor guiGraphics, Font font, BingoBoardClientState board, IntPredicate completed, boolean showCompletions, int boardX, int boardY) {
		int rows = board.rows();
		int columns = board.columns();
		List<Optional<BingoBoardClientState.Tile>> tiles = board.tiles();

		// Full board background
		guiGraphics.fill(boardX - 3, boardY - 3, boardX + getCompactBoardWidth(board) + 3, boardY + getCompactBoardHeight(board) + 3, BACKGROUND_COLOR);

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
				boolean tileCompleted = completed.test(index);
				drawTileBackground(guiGraphics, tileCompleted, x, y, TILE_SIZE, TILE_SIZE);

				int iconX = x + (TILE_SIZE - 16) / 2;
				int iconY = y + (TILE_SIZE - 16) / 2;
				if (!tile.icon().isEmpty()) {
					guiGraphics.item(tile.icon(), iconX, iconY);
					guiGraphics.itemDecorations(font, tile.icon(), iconX, iconY);
				}

				// How many players beat us to it, in the corner away from the item count
				if (showCompletions && !tileCompleted && tile.completions() > 0) {
					Matrix3x2fStack pose = guiGraphics.pose();
					pose.pushMatrix();
					pose.translate(x + 2, y + 2);
					pose.scale(0.5f, 0.5f);
					guiGraphics.text(font, String.valueOf(tile.completions()), 0, 0, POINTS_COLOR, true);
					pose.popMatrix();
				}
			}
		}
	}

	/// Draws a bar of how many of the unlocked tiles each player has completed, ordered by points
	private static void drawStandings(GuiGraphicsExtractor guiGraphics, Font font, BingoBoardClientState board, int left, int top, float partialTick) {
		List<BingoBoardClientState.PlayerProgress> players = STANDINGS.players();
		// Nobody to compare ourselves with
		if (players.size() < 2) {
			return;
		}

		int pointsWidth = 0;
		for (BingoBoardClientState.PlayerProgress player : players) {
			pointsWidth = Math.max(pointsWidth, font.width(String.valueOf(player.points())));
		}
		int barX = left + FACE_SIZE + STANDINGS_GAP;
		int pointsX = barX + BAR_WIDTH + STANDINGS_GAP;
		int right = pointsX + pointsWidth;
		int bottom = top + (players.size() - 1) * STANDINGS_ROW_HEIGHT + FACE_SIZE;
		guiGraphics.fill(left - 3, top - 3, right + 3, bottom + 3, BACKGROUND_COLOR);

		int unlockedTiles = board.unlockedTileCount();
		LocalPlayer localPlayer = Minecraft.getInstance().player;
		for (BingoBoardClientState.PlayerProgress player : players) {
			int y = top + Math.round(STANDINGS.position(player.id(), partialTick) * STANDINGS_ROW_HEIGHT);
			boolean self = localPlayer != null && player.id().equals(localPlayer.getUUID());

			PlayerFaceExtractor.extractRenderState(guiGraphics, ClientPlayerInfo.getSkin(player.id()), left, y, FACE_SIZE);

			int barY = y + (FACE_SIZE - BAR_HEIGHT) / 2;
			if (self) {
				drawBorder(guiGraphics, barX - 1, barY - 1, BAR_WIDTH + 2, BAR_HEIGHT + 2, BORDER_COLOR);
			}
			guiGraphics.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, TILE_COLOR);
			if (unlockedTiles > 0) {
				int filled = BAR_WIDTH * Math.min(player.completedTiles().size(), unlockedTiles) / unlockedTiles;
				guiGraphics.fill(barX, barY, barX + filled, barY + BAR_HEIGHT, COMPLETED_COLOR);
			}

			guiGraphics.text(font, String.valueOf(player.points()), pointsX, y, self ? OWN_POINTS_COLOR : POINTS_COLOR, true);
		}
	}

	public static void drawTileBackground(GuiGraphicsExtractor guiGraphics, BingoBoardClientState.Tile tile, int x, int y, int w, int h) {
		drawTileBackground(guiGraphics, tile.completed(), x, y, w, h);
	}

	public static void drawTileBackground(GuiGraphicsExtractor guiGraphics, boolean completed, int x, int y, int w, int h) {
		guiGraphics.fill(x, y, x + w, y + h, completed ? COMPLETED_COLOR : TILE_COLOR);
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

	public static void drawBorder(GuiGraphicsExtractor guiGraphics, int x, int y, int w, int h, int color) {
		guiGraphics.fill(x, y, x + w, y + 1, color);
		guiGraphics.fill(x, y + h - 1, x + w, y + h, color);
		guiGraphics.fill(x, y, x + 1, y + h, color);
		guiGraphics.fill(x + w - 1, y, x + w, y + h, color);
	}
}
