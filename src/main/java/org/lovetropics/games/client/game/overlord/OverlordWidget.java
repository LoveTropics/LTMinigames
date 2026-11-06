package org.lovetropics.games.client.game.overlord;

import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import org.joml.Matrix3x2fStack;
import org.lovetropics.games.client.game.bingo.BingoBoardScreen;
import org.lovetropics.games.client.game.handler.GameBingoHandler;
import org.lovetropics.games.client.screen.ClientPlayerInfo;
import org.lovetropics.games.common.content.bingo.Bingo;
import org.lovetropics.games.common.content.shop.ShopTexts;
import org.lovetropics.games.common.content.tropic_rush.TropicRushTexts;
import org.lovetropics.games.common.core.game.client_state.instance.BingoBoardClientState;
import org.lovetropics.games.common.core.game.client_state.instance.BingoBoardClientState.PlayerProgress;
import org.lovetropics.games.common.core.game.client_state.instance.ShopClientState;

import java.util.List;

enum OverlordWidget {
	POINTS(TropicRushTexts.OVERLORD_POINTS) {
		@Override
		int draw(GuiGraphicsExtractor graphics, Font font, OverlordScreen.Data data, int x, int y, int w, int h, int scroll) {
			List<PlayerProgress> players = data.players();
			if (players.isEmpty()) {
				return drawEmpty(graphics, font, TropicRushTexts.OVERLORD_NO_PLAYERS, x, y);
			}
			int unlockedTiles = data.board().unlockedTileCount();
			int visible = Math.max(h / ROW_H, 1);
			for (int row = 0; row < visible && scroll + row < players.size(); row++) {
				int rank = scroll + row;
				PlayerProgress player = players.get(rank);
				int rowY = y + row * ROW_H;
				PlayerFaceExtractor.extractRenderState(graphics, ClientPlayerInfo.getSkin(player.id()), x, rowY, FACE_SIZE);

				Component progress = Bingo.BOARD_HEADER.apply(player.points(), player.completedTiles().size(), unlockedTiles).withStyle(ChatFormatting.GOLD);
				int progressX = x + w - font.width(progress);
				graphics.text(font, progress, progressX, rowY, CommonColors.WHITE, true);

				String place = "#" + (rank + 1) + " ";
				int placeX = x + FACE_SIZE + 3;
				graphics.text(font, place, placeX, rowY, CommonColors.GRAY, true);
				int nameX = placeX + font.width(place);
				graphics.text(font, font.plainSubstrByWidth(player.name(), progressX - 4 - nameX), nameX, rowY, CommonColors.WHITE, true);
			}
			return Math.max(players.size() - visible, 0);
		}
	},
	BOARDS(TropicRushTexts.OVERLORD_BOARDS) {
		@Override
		int draw(GuiGraphicsExtractor graphics, Font font, OverlordScreen.Data data, int x, int y, int w, int h, int scroll) {
			List<PlayerProgress> players = data.players();
			if (players.isEmpty()) {
				return drawEmpty(graphics, font, TropicRushTexts.OVERLORD_NO_PLAYERS, x, y);
			}
			BingoBoardClientState board = data.board();
			int boardW = GameBingoHandler.getCompactBoardWidth(board) + 6;
			int boardH = GameBingoHandler.getCompactBoardHeight(board) + 6;

			int columns = 1;
			float scale = 0.0f;
			for (int c = 1; c <= players.size(); c++) {
				int r = Mth.positiveCeilDiv(players.size(), c);
				float s = Math.min((float) (w - (c - 1) * GAP) / (c * boardW), (float) (h - r * ROW_H - (r - 1) * GAP) / (r * boardH));
				if (s > scale) {
					scale = s;
					columns = c;
				}
			}
			if (scale <= 0.0f) {
				return 0;
			}
			scale = Math.min(scale, 1.0f);

			int cellW = Math.round(boardW * scale);
			int cellH = ROW_H + Math.round(boardH * scale);
			int left = x + (w - columns * cellW - (columns - 1) * GAP) / 2;
			for (int index = 0; index < players.size(); index++) {
				PlayerProgress player = players.get(index);
				int cellX = left + (index % columns) * (cellW + GAP);
				int cellY = y + (index / columns) * (cellH + GAP);
				PlayerFaceExtractor.extractRenderState(graphics, ClientPlayerInfo.getSkin(player.id()), cellX, cellY, FACE_SIZE);
				graphics.text(font, font.plainSubstrByWidth(player.name(), cellW - FACE_SIZE - 2), cellX + FACE_SIZE + 2, cellY, CommonColors.WHITE, true);

				Matrix3x2fStack pose = graphics.pose();
				pose.pushMatrix();
				pose.translate(cellX, cellY + ROW_H);
				pose.scale(scale, scale);
				GameBingoHandler.drawCompactBoard(graphics, font, board, player::hasCompleted, false, 3, 3);
				pose.popMatrix();
			}
			return 0;
		}

		@Override
		Screen open(Screen parent) {
			return new BingoBoardScreen(parent);
		}
	},
	SABOTAGES(TropicRushTexts.OVERLORD_SABOTAGES) {
		@Override
		int draw(GuiGraphicsExtractor graphics, Font font, OverlordScreen.Data data, int x, int y, int w, int h, int scroll) {
			List<ShopClientState.Sabotage> sabotages = data.sabotages();
			if (sabotages.isEmpty()) {
				return drawEmpty(graphics, font, TropicRushTexts.OVERLORD_NO_SABOTAGES, x, y);
			}
			int rowH = font.lineHeight + 1;
			int visible = Math.max(h / rowH, 1);
			for (int row = 0; row < visible && scroll + row < sabotages.size(); row++) {
				// newest first
				ShopClientState.Sabotage sabotage = sabotages.get(sabotages.size() - 1 - scroll - row);
				int rowY = y + row * rowH;
				String time = formatTicks(sabotage.ticks());
				graphics.text(font, time, x, rowY, CommonColors.GRAY, true);

				int textX = x + font.width(time) + 4;
				Component text = ShopTexts.USE_ON_BROADCAST.apply(sabotage.buyer(), sabotage.offer().copy().withStyle(ChatFormatting.AQUA), sabotage.target());
				List<FormattedCharSequence> lines = font.split(text, x + w - textX);
				if (!lines.isEmpty()) {
					graphics.text(font, lines.getFirst(), textX, rowY, CommonColors.WHITE, true);
				}
			}
			return Math.max(sabotages.size() - visible, 0);
		}
	},
	COMPLETIONS(TropicRushTexts.OVERLORD_COMPLETIONS) {
		@Override
		int draw(GuiGraphicsExtractor graphics, Font font, OverlordScreen.Data data, int x, int y, int w, int h, int scroll) {
			List<PlayerProgress> players = data.players();
			if (players.isEmpty()) {
				return drawEmpty(graphics, font, TropicRushTexts.OVERLORD_NO_PLAYERS, x, y);
			}
			int maxTicks = 1;
			int maxCount = 1;
			int legendW = 0;
			for (PlayerProgress player : players) {
				List<Integer> ticks = player.completionTicks();
				if (!ticks.isEmpty()) {
					maxTicks = Math.max(maxTicks, ticks.getLast());
				}
				maxCount = Math.max(maxCount, ticks.size());
				legendW = Math.max(legendW, font.width(player.name()));
			}

			String countLabel = String.valueOf(maxCount);
			int graphX = x + font.width(countLabel) + 3;
			int graphW = x + w - legendW - 6 - graphX;
			int graphH = h - font.lineHeight - 3;
			if (graphW <= 1 || graphH <= 1) {
				return 0;
			}
			int bottom = y + graphH - 1;
			graphics.fill(graphX - 1, y, graphX, y + graphH + 1, AXIS_COLOR);
			graphics.fill(graphX - 1, y + graphH, graphX + graphW, y + graphH + 1, AXIS_COLOR);
			graphics.text(font, countLabel, x, y, CommonColors.GRAY, true);
			String endLabel = formatTicks(maxTicks);
			graphics.text(font, formatTicks(0), graphX, y + graphH + 3, CommonColors.GRAY, true);
			graphics.text(font, endLabel, graphX + graphW - font.width(endLabel), y + graphH + 3, CommonColors.GRAY, true);

			// autocomplete did the whole loop i have to check later I haven't got the time RN
			for (int index = 0; index < players.size(); index++) {
				PlayerProgress player = players.get(index);
				int color = PLAYER_COLORS[data.board().players().indexOf(player) % PLAYER_COLORS.length];

				int lastX = graphX;
				int lastY = bottom;
				List<Integer> ticks = player.completionTicks();
				for (int count = 1; count <= ticks.size(); count++) {
					int pointX = graphX + (int) ((long) ticks.get(count - 1) * (graphW - 1) / maxTicks);
					int pointY = bottom - count * (graphH - 1) / maxCount;
					graphics.fill(lastX, lastY, pointX + 1, lastY + 1, color);
					graphics.fill(pointX, pointY, pointX + 1, lastY + 1, color);
					lastX = pointX;
					lastY = pointY;
				}
				graphics.fill(lastX, lastY, graphX + graphW, lastY + 1, color);

				int legendY = y + index * font.lineHeight;
				if (legendY + font.lineHeight <= y + h) {
					graphics.text(font, player.name(), x + w - legendW, legendY, color, true);
				}
			}
			return 0;
		}
	};

	private static final int FACE_SIZE = GameBingoHandler.FACE_SIZE;
	private static final int ROW_H = FACE_SIZE + 2;
	private static final int GAP = 4;
	private static final int AXIS_COLOR = 0xFF555555;
	private static final int[] PLAYER_COLORS = {0xFF55FFFF, 0xFFFF5555, 0xFF55FF55, 0xFFFFFF55, 0xFFFF55FF, 0xFFFFAA00, 0xFF5555FF, 0xFFFFFFFF};

	final Component title;

	OverlordWidget(Component title) {
		this.title = title;
	}

	abstract int draw(GuiGraphicsExtractor graphics, Font font, OverlordScreen.Data data, int x, int y, int w, int h, int scroll);

	Screen open(Screen parent) {
		return new OverlordDetailScreen(this, parent);
	}

	private static int drawEmpty(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y) {
		graphics.text(font, text, x, y, CommonColors.GRAY, true);
		return 0;
	}

	private static String formatTicks(int ticks) {
		return StringUtil.formatTickDuration(ticks, SharedConstants.TICKS_PER_SECOND);
	}
}
