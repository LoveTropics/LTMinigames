package org.lovetropics.games.client.game.bingo;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.objects.PlayerSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.component.ResolvableProfile;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.client.LTKeybinds;
import org.lovetropics.games.client.game.handler.GameBingoHandler;
import org.lovetropics.games.client.screen.ClientPlayerInfo;
import org.lovetropics.games.common.content.bingo.Bingo;
import org.lovetropics.games.common.content.bingo.BingoTileType;
import org.lovetropics.games.common.core.game.client_state.instance.BingoBoardClientState;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/// The full bingo board, with tooltips giving the details of each tile
public final class BingoBoardScreen extends Screen {
	private static final int TILE_W = 52;
	private static final int GAP = 4;
	private static final int ICON_SIZE = 20;
	private static final int KIND_SPRITE_SIZE = 14;
	private static final float KIND_SPRITE_ALPHA = 0.2f;
	private static final int TOP_PADDING = 3;
	private static final int SIDE_PADDING = 3;
	private static final int ICON_KIND_GAP = 2;
	private static final int COMPLETED_POINTS_COLOR = 0xFF55FF55;
	private static final int MULTIPLIED_PREVIOUS_POINTS_COLOR = 0xFFAAAAAA;
	private static final int MULTIPLIED_COLUMN_BORDER = 0xFFFFDD33;
	private static final int MULTIPLIED_COLUMN_FILL = 0x33FFDD33;
	private static final DecimalFormat MULTIPLIER_FORMAT = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ROOT));
	private static final int BOTTOM_PADDING = 3;
	private static final int SCREEN_MARGIN = 10;
	private static final int TOOLTIP_WIDTH = 200;
	private static final float CARD_SCALE = 0.5f;
	private static final int CARD_HEADER_H = GameBingoHandler.FACE_SIZE + 3;
	private static final int CARD_GAP = 6;
	private static final int PANEL_GAP = 12;

	private static final Style POINTS_STYLE = Style.EMPTY.withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath("lt", "minecraft_five")));

	/// The other players, whose boards are shown next to ours
	private final BingoStandings standings = new BingoStandings(false);

	public BingoBoardScreen() {
		super(Bingo.BOARD_TITLE);
		standings.tick(GameBingoHandler.board());
	}

	@Override
	public void tick() {
		super.tick();
		standings.tick(GameBingoHandler.board());
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

		int lineHeight = font.lineHeight;
		int tileH = TOP_PADDING + ICON_SIZE + ICON_KIND_GAP + KIND_SPRITE_SIZE + BOTTOM_PADDING;

		// spectators can still view other bingos
		int boardW = board.hasCard() ? columns * TILE_W + (columns - 1) * GAP : 0;
		int boardH = board.hasCard() ? rows * tileH + (rows - 1) * GAP : 0;
		int panelGap = board.hasCard() ? PANEL_GAP : 0;

		// The other players' boards go in a panel next to ours
		List<BingoBoardClientState.PlayerProgress> others = standings.players();
		int cardW = Math.round((GameBingoHandler.getCompactBoardWidth(board) + 6) * CARD_SCALE);
		int cardH = Math.round((GameBingoHandler.getCompactBoardHeight(board) + 6) * CARD_SCALE);
		int cellH = CARD_HEADER_H + cardH;
		int panelColumns = board.hasCard() ? (others.size() > 3 ? 2 : 1) : Mth.clamp(others.size(), 1, 3);
		int panelRows = Mth.positiveCeilDiv(others.size(), panelColumns);
		int panelW = others.isEmpty() ? 0 : panelGap + panelColumns * cardW + (panelColumns - 1) * CARD_GAP;
		int panelH = others.isEmpty() ? 0 : panelRows * cellH + (panelRows - 1) * CARD_GAP;

		Component header = board.hasCard() ? createHeader(board) : CommonComponents.EMPTY;
		int headerH = lineHeight * 2 + GAP * 2;
		int mainW = boardW + panelW;
		int contentW = Math.max(mainW, font.width(header));
		int contentH = headerH + Math.max(boardH, panelH);

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

		int boardX = (contentW - mainW) / 2;
		int boardY = headerH;
		if (board.hasCard()) {
			graphics.fill(boardX - 3, boardY - 3, boardX + boardW + 3, boardY + boardH + 3, 0x66000000);
		}

		@Nullable List<FormattedCharSequence> tooltip = null;

		int panelX = boardX + boardW + panelGap;
		for (BingoBoardClientState.PlayerProgress player : others) {
			// Players overtaking each other slide between their places
			float position = standings.position(player.id(), a);
			int from = Mth.floor(position);
			int to = Math.min(from + 1, others.size() - 1);
			float progress = position - from;
			float cardX = panelX + Mth.lerp(progress, (from % panelColumns) * (cardW + CARD_GAP), (to % panelColumns) * (cardW + CARD_GAP));
			float cardY = boardY + Mth.lerp(progress, (from / panelColumns) * (cellH + CARD_GAP), (to / panelColumns) * (cellH + CARD_GAP));

			drawPlayerCard(graphics, board, player, cardX, cardY, cardW);
			if (localMouseX >= cardX && localMouseX < cardX + cardW && localMouseY >= cardY && localMouseY < cardY + cellH) {
				tooltip = createPlayerTooltip(board, player);
			}
		}

		if (board.hasCard()) {
			boolean[] completedMultiplierColumns = new boolean[columns];
			boolean[] boostedColumns = new boolean[columns];
			for (int index = 0; index < tiles.size(); index++) {
				BingoBoardClientState.Tile tile = tiles.get(index).orElse(null);
				if (tile == null || !tile.isColumnMultiplier()) {
					continue;
				}
				int thing = index % columns;
				int x = boardX + thing * (TILE_W + GAP);
				int y = boardY + (index / columns) * (tileH + GAP);
				boolean hovered = localMouseX >= x && localMouseX < x + TILE_W && localMouseY >= y && localMouseY < y + tileH;
				completedMultiplierColumns[thing] |= tile.completed();
				boostedColumns[thing] |= tile.completed() || hovered;
			}

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
							drawLockedTile(graphics, x, y, tileH);
							if (hovered) {
								tooltip = createLockedTooltip(board, lockedSlot.get());
							}
						}
						continue;
					}

					BingoBoardClientState.Tile tile = optionalTile.orElseThrow();
					drawTile(graphics, tile, x, y, tileH, boostedColumns[c]);
					if (hovered) {
						tooltip = createTooltip(board, tile);
					}
				}
			}

			for (int c = 0; c < columns; c++) {
				if (!boostedColumns[c]) {
					continue;
				}
				int x = boardX + c * (TILE_W + GAP);
				if (completedMultiplierColumns[c]) {
					graphics.fill(x, boardY, x + TILE_W, boardY + boardH, MULTIPLIED_COLUMN_FILL);
				}
				GameBingoHandler.drawBorder(graphics, x - 2, boardY - 2, TILE_W + 4, boardH + 4, MULTIPLIED_COLUMN_BORDER);
			}
		}

		pose.popMatrix();

		if (tooltip != null) {
			graphics.setTooltipForNextFrame(font, tooltip, mouseX, mouseY);
		}
	}

	/// @param boosted whether to show what the tile is worth with the multipliers of its column
	private void drawTile(GuiGraphicsExtractor graphics, BingoBoardClientState.Tile tile, int x, int y, int tileH, boolean boosted) {
		GameBingoHandler.drawTileBackground(graphics, tile, tile.completed(), x, y, TILE_W, tileH);

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

		int rowY = iconY + ICON_SIZE + ICON_KIND_GAP;

		// what the tile earned the player once completed, otherwise what it is worth now
		int pointsX = x + SIDE_PADDING;
		int pointsY = rowY + (KIND_SPRITE_SIZE - font.lineHeight) / 2 + 1;
		int pointsColor = tile.completed() ? COMPLETED_POINTS_COLOR : GameBingoHandler.POINTS_COLOR;
		if (boosted && tile.boostedReward() != tile.reward()) {
			Component previous = Component.literal(String.valueOf(tile.reward())).withStyle(POINTS_STYLE.withStrikethrough(true));
			graphics.text(font, previous, pointsX, pointsY, MULTIPLIED_PREVIOUS_POINTS_COLOR, true);
			pointsX += font.width(previous) + 2;
			Component points = Component.literal(String.valueOf(tile.boostedReward())).withStyle(POINTS_STYLE);
			graphics.text(font, points, pointsX, pointsY, pointsColor, true);
		} else {
			Component points = Component.literal(String.valueOf(tile.reward())).withStyle(POINTS_STYLE);
			graphics.text(font, points, pointsX, pointsY, pointsColor, true);
		}

		int kindX = x + TILE_W - SIDE_PADDING - KIND_SPRITE_SIZE;
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, tile.kind().sprite(), kindX, rowY, KIND_SPRITE_SIZE, KIND_SPRITE_SIZE, KIND_SPRITE_ALPHA);
	}

	/// Draws another player's head, name and points above a compact version of their board
	private void drawPlayerCard(GuiGraphicsExtractor graphics, BingoBoardClientState board, BingoBoardClientState.PlayerProgress player, float x, float y, int cardW) {
		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(x, y);

		PlayerFaceExtractor.extractRenderState(graphics, ClientPlayerInfo.getSkin(player.id()), 0, 0, GameBingoHandler.FACE_SIZE);
		String points = String.valueOf(player.points());
		int pointsW = font.width(points);
		int nameX = GameBingoHandler.FACE_SIZE + 2;
		String name = font.plainSubstrByWidth(player.name(), cardW - nameX - pointsW - 2);
		graphics.text(font, name, nameX, 0, 0xFFFFFFFF, true);
		graphics.text(font, points, cardW - pointsW, 0, GameBingoHandler.POINTS_COLOR, true);

		pose.translate(0, CARD_HEADER_H);
		pose.scale(CARD_SCALE, CARD_SCALE);
		// The compact board draws its background around it
		GameBingoHandler.drawCompactBoard(graphics, font, board, player::hasCompleted, false, 3, 3);

		pose.popMatrix();
	}

	private void drawLockedTile(GuiGraphicsExtractor graphics, int x, int y, int tileH) {
		GameBingoHandler.drawLockedBackground(graphics, BingoTileType.DEFAULT.lockedColor(), BingoTileType.DEFAULT.lockedBorderColor(), x, y, TILE_W, tileH);
		// Keep the question mark where the icon of other tiles is
		GameBingoHandler.drawLockedIcon(graphics, font, x, y + TOP_PADDING, TILE_W, ICON_SIZE);
	}

	private List<FormattedCharSequence> createTooltip(BingoBoardClientState board, BingoBoardClientState.Tile tile) {
		List<FormattedCharSequence> lines = new ArrayList<>();
		lines.addAll(font.split(tile.title().copy().withStyle(ChatFormatting.YELLOW), TOOLTIP_WIDTH));
		tile.description().ifPresent(description -> lines.addAll(font.split(description.copy().withStyle(ChatFormatting.GRAY), TOOLTIP_WIDTH)));
		lines.add(CommonComponents.EMPTY.getVisualOrderText());
		for (int place = 0; place < tile.placeRewards().size(); place++) {
			lines.add(createPointsLine(board, tile, place).getVisualOrderText());
		}
		return lines;
	}

	// points for each place, crossed out if someone got it
	private static Component createPointsLine(BingoBoardClientState board, BingoBoardClientState.Tile tile, int place) {
		Component placeName = place < Bingo.TILE_PLACES.size() ? Bingo.TILE_PLACES.get(place) : Bingo.TILE_PLACE.apply(place + 1);
		MutableComponent reward = Bingo.TILE_PLACE_REWARD.apply(placeName, tile.placeRewards().get(place));
		if (tile.isColumnMultiplier()) {
			float multiplier = tile.columnMultipliers().get(Math.min(place, tile.columnMultipliers().size() - 1));
			reward.append(Component.literal(" ")).append(Bingo.TILE_PLACE_COLUMN_MULTIPLIER.apply(MULTIPLIER_FORMAT.format(multiplier)));
		}
		if (place >= tile.completedBy().size()) {
			return reward.withStyle(place == tile.completedBy().size() ? ChatFormatting.GOLD : ChatFormatting.GRAY);
		}

		UUID player = tile.completedBy().get(place);
		boolean self = Minecraft.getInstance().player != null && player.equals(Minecraft.getInstance().player.getUUID());
		return Component.empty()
				.append(reward.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.STRIKETHROUGH))
				.append(Component.literal(" "))
				.append(Component.object(new PlayerSprite(ResolvableProfile.createUnresolved(player), true)))
				.append(Component.literal(" ").append(getPlayerName(board, player)).withStyle(self ? ChatFormatting.GREEN : ChatFormatting.WHITE));
	}

	private static Component getPlayerName(BingoBoardClientState board, UUID player) {
		for (BingoBoardClientState.PlayerProgress progress : board.players()) {
			if (progress.id().equals(player)) {
				return Component.literal(progress.name());
			}
		}
		Component name = ClientPlayerInfo.getName(player);
		return name != null ? name : Component.literal("?");
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

	private List<FormattedCharSequence> createPlayerTooltip(BingoBoardClientState board, BingoBoardClientState.PlayerProgress player) {
		return List.of(
				Component.literal(player.name()).withStyle(ChatFormatting.AQUA).getVisualOrderText(),
				Bingo.BOARD_HEADER.apply(player.points(), player.completedTiles().size(), board.unlockedTileCount()).withStyle(ChatFormatting.GOLD).getVisualOrderText()
		);
	}

	private static Component createHeader(BingoBoardClientState board) {
		int unlocked = board.unlockedTileCount();
		int completed = (int) board.tiles().stream().flatMap(Optional::stream).filter(BingoBoardClientState.Tile::completed).count();
		MutableComponent header = Bingo.BOARD_HEADER.apply(board.points(), completed, unlocked).withStyle(ChatFormatting.GOLD);
		if (!board.lockedSlots().isEmpty()) {
			header.append(Component.literal(" - ").withStyle(ChatFormatting.GRAY))
					.append(Bingo.BOARD_LOCKED_COUNT.apply(board.lockedSlots().size()).withStyle(ChatFormatting.GRAY));
		}
		return header;
	}
}
