package com.lovetropics.minigames.client.game.handler;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.client_state.instance.BingoBoardClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public class GameBingoHandler {
	private static final int TILE_W = 64;
	private static final int GAP = 4;

	private static final int ICON_SIZE = 20;
	private static final int TOP_PADDING = 3;
	private static final int SIDE_PADDING = 3;
	private static final int ICON_TEXT_GAP = 2;
	private static final int BOTTOM_PADDING = 3;
	private static final int COMPACT_TILE_SIZE = 16 + 2 * 2; // 2px padding

	private static int rows;
	private static int columns;
	@Nullable
	private static List<Optional<BingoBoardClientState.Tile>> tiles;

	static final ClientGameStateHandler<BingoBoardClientState> HANDLER = new ClientGameStateHandler<>() {
		@Override
		public void accept(BingoBoardClientState state) {
			rows = state.rows();
			columns = state.columns();
			tiles = state.tiles();
		}

		@Override
		public void disable(BingoBoardClientState state) {
			rows = 0;
			columns = 0;
			tiles = null;
		}
	};

	@SubscribeEvent
	static void registerLayers(RegisterGuiLayersEvent event) {
		event.registerAboveAll(Identifier.fromNamespaceAndPath(LoveTropics.ID, "bingo_board"), (guiGraphics, deltaTracker) -> {
			if (rows <= 0 || columns <= 0 || tiles == null) return;

			Minecraft mc = Minecraft.getInstance();
			Font font = mc.font;

			int boardX = 20, boardY = 20;

			int textAreaW = TILE_W - SIDE_PADDING * 2;

			// Calculate maximum amount of lines needed so that they all tiles are equal
			int maxLines = tiles.stream()
					.flatMap(Optional::stream)
					.map(t -> font.split(t.title(), textAreaW).size())
					.max(Comparator.naturalOrder()).orElse(1);

			boolean compact = !Minecraft.getInstance().hasShiftDown();

			int lineHeight = font.lineHeight;
			int textAreaH = maxLines * lineHeight;

			int tileW = compact ? COMPACT_TILE_SIZE : TILE_W;
			int tileH = compact ? COMPACT_TILE_SIZE : TOP_PADDING + ICON_SIZE + ICON_TEXT_GAP + textAreaH + BOTTOM_PADDING;

			int boardW = columns * tileW + (columns - 1) * GAP;
			int boardH = rows * tileH + (rows - 1) * GAP;

			// Full baord background
			guiGraphics.fill(boardX - 3, boardY - 3, boardX + boardW + 3, boardY + boardH + 3, 0x66000000);

			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < columns; c++) {
					int x = boardX + c * (tileW + GAP);
					int y = boardY + r * (tileH + GAP);

					var optionalTile = tiles.get((r * rows) + c);
					if (optionalTile.isEmpty()) continue;

					var tile = optionalTile.orElseThrow();

					// Tile border and background
					guiGraphics.fill(x, y, x + tileW, y + tileH, tile.completed() ? 0xAA61be29 : 0xAA222222);
					guiGraphics.fill(x, y, x + tileW, y + 1, 0xFFFFFFFF);
					guiGraphics.fill(x, y + tileH - 1, x + tileW, y + tileH, 0xFFFFFFFF);
					guiGraphics.fill(x, y, x + 1, y + tileH, 0xFFFFFFFF);
					guiGraphics.fill(x + tileW - 1, y, x + tileW, y + tileH, 0xFFFFFFFF);

					if (compact) {
						int iconX = x + (tileW - 16) / 2;
						int iconY = y + (tileH - 16) / 2;

						if (!tile.icon().isEmpty()) {
							guiGraphics.item(tile.icon(), iconX, iconY);
							guiGraphics.itemDecorations(font, tile.icon(), iconX, iconY);
						}

						continue;
					}

					int iconX = x + (TILE_W - ICON_SIZE) / 2;
					int iconY = y + TOP_PADDING;

					if (!tile.icon().isEmpty()) {
						var pose = guiGraphics.pose();
						pose.pushMatrix();
						float scale = ICON_SIZE / 16.0f;
						pose.translate(iconX, iconY);
						pose.scale(scale, scale);
						guiGraphics.item(tile.icon(), 0, 0);
						pose.popMatrix();

						guiGraphics.itemDecorations(font, tile.icon(), iconX, iconY);
					}

					int textAreaX = x + SIDE_PADDING;
					int textAreaY = iconY + ICON_SIZE + ICON_TEXT_GAP;

					var lines = font.split(tile.title(), textAreaW);
					int offset = (maxLines - lines.size()) * lineHeight / 2;

					for (int i = 0; i < lines.size(); i++) {
						var line = lines.get(i);
						int lw = font.width(line);
						int lx = textAreaX + (textAreaW - lw) / 2;
						int ly = textAreaY + i * lineHeight + offset;
						guiGraphics.text(font, line, lx, ly, 0xFFFFFFFF, false);
					}
				}
			}
		});
	}
}
