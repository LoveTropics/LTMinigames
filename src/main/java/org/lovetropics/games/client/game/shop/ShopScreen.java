package org.lovetropics.games.client.game.shop;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Matrix3x2fStack;
import org.lovetropics.games.client.LTKeybinds;
import org.lovetropics.games.client.game.handler.GameShopHandler;
import org.lovetropics.games.client.screen.ClientPlayerInfo;
import org.lovetropics.games.common.content.mcsr.McsrTexts;
import org.lovetropics.games.common.content.mcsr.shop.ShopTarget;
import org.lovetropics.games.common.core.game.client_state.instance.ShopClientState;
import org.lovetropics.games.common.core.network.shop.ServerboundShopPurchasePacket;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/// Lets players spend their tropicoins on the offers of the shop.
///
/// Offers that are used on a player of the buyer's choice first show the other players, with the most points first.
public final class ShopScreen extends Screen {
	private static final int CARD_W = 72;
	private static final int GAP = 4;
	private static final int ICON_SIZE = 20;
	private static final int PADDING = 3;
	private static final int ICON_TEXT_GAP = 2;
	private static final int MAX_COLUMNS = 5;
	private static final int SCREEN_MARGIN = 10;
	private static final int TOOLTIP_WIDTH = 200;
	private static final int ROW_W = 160;
	private static final int FACE_SIZE = 10;

	private static final int CARD_COLOR = 0xAA222222;
	private static final int CARD_HOVERED_COLOR = 0xAA444444;
	private static final int CARD_UNAFFORDABLE_COLOR = 0x66000000;
	private static final int BORDER_COLOR = 0xFF555555;
	private static final int BORDER_HOVERED_COLOR = 0xFFFFFFFF;
	private static final int BENEFIT_BORDER_COLOR = 0xFF33AA33;
	private static final int BENEFIT_BORDER_HOVERED_COLOR = 0xFF55FF55;
	private static final int SABOTAGE_BORDER_COLOR = 0xFFAA3333;
	private static final int SABOTAGE_BORDER_HOVERED_COLOR = 0xFFFF5555;

	// Where the offers were last drawn, to know which one gets clicked
	private float scale = 1.0f;
	private float originX, originY;
	private int gridX, gridY;
	private int columns = 1;
	private int cardH;

	/// The offer the player is picking a target for, or `-1` while browsing the offers
	private int pickingOffer = -1;
	// Where the rows of the target picker were last drawn
	private int rowsX, rowsY, rowH;

	public ShopScreen() {
		super(McsrTexts.SHOP);
	}

	@Override
	public void tick() {
		super.tick();
		ShopClientState shop = GameShopHandler.shop();
		// The game is over, or we left it
		if (shop == null) {
			super.onClose();
			return;
		}
		// e.g. the last other player left while picking
		if (pickingOffer >= 0 && (pickingOffer >= shop.offers().size() || !canBuy(shop, shop.offers().get(pickingOffer)))) {
			pickingOffer = -1;
		}
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (LTKeybinds.OPEN_SHOP.matches(event)) {
			super.onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	/// Escape leaves the target picker first
	@Override
	public void onClose() {
		if (pickingOffer >= 0) {
			pickingOffer = -1;
		} else {
			super.onClose();
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		ShopClientState shop = GameShopHandler.shop();
		if (shop != null && event.input() == 0) {
			if (pickingOffer >= 0) {
				clickTargetRow(shop, getRowAt(shop, event.x(), event.y()));
				return true;
			}
			int index = getOfferAt(shop, event.x(), event.y());
			if (index >= 0 && canBuy(shop, shop.offers().get(index))) {
				if (shop.offers().get(index).target() == ShopTarget.PICK) {
					pickingOffer = index;
				} else {
					purchase(index, Optional.empty());
				}
				playClickSound();
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	/// @param row the row of the target picker, as given by [#getRowAt]
	private void clickTargetRow(ShopClientState shop, int row) {
		int offer = pickingOffer;
		pickingOffer = -1;
		// Clicking the cancel row or outside of the rows cancels
		if (row < 0 || row > shop.targets().size()) {
			return;
		}
		// The row after the players picks one at random
		Optional<UUID> target = row < shop.targets().size() ? Optional.of(shop.targets().get(row).id()) : Optional.empty();
		purchase(offer, target);
		playClickSound();
	}

	private static void purchase(int offer, Optional<UUID> target) {
		// The server checks the balance and target again, and sends the new balance back
		ClientPacketDistributor.sendToServer(new ServerboundShopPurchasePacket(offer, target));
	}

	private void playClickSound() {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
	}

	private static boolean canBuy(ShopClientState shop, ShopClientState.Offer offer) {
		return shop.balance() >= offer.cost() && hasTarget(shop, offer);
	}

	private static boolean hasTarget(ShopClientState shop, ShopClientState.Offer offer) {
		return !offer.target().isOtherPlayer() || !shop.targets().isEmpty();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);

		ShopClientState shop = GameShopHandler.shop();
		if (shop == null || shop.offers().isEmpty()) {
			return;
		}
		if (pickingOffer >= 0 && pickingOffer < shop.offers().size()) {
			drawTargetPicker(graphics, shop, shop.offers().get(pickingOffer), mouseX, mouseY);
			return;
		}
		List<ShopClientState.Offer> offers = shop.offers();

		int textAreaW = CARD_W - PADDING * 2;
		int lineHeight = font.lineHeight;

		// All cards are as tall as the one with the longest name
		int maxLines = offers.stream()
				.map(offer -> font.split(offer.name(), textAreaW).size())
				.max(Comparator.naturalOrder()).orElse(1);
		cardH = PADDING + ICON_SIZE + ICON_TEXT_GAP + maxLines * lineHeight + ICON_TEXT_GAP + lineHeight + PADDING;

		columns = Math.min(MAX_COLUMNS, offers.size());
		int rows = (offers.size() + columns - 1) / columns;
		int gridW = columns * CARD_W + (columns - 1) * GAP;
		int gridH = rows * cardH + (rows - 1) * GAP;

		Component balance = McsrTexts.TROPICOINS.apply(shop.balance()).withStyle(ChatFormatting.GOLD);
		int headerH = lineHeight * 2 + GAP * 2;
		int contentW = Math.max(gridW, Math.max(font.width(title), font.width(balance)));
		int contentH = headerH + gridH;

		// Shrink the shop on small screens rather than cutting it off
		scale = Math.min(1.0f, Math.min((float) (width - SCREEN_MARGIN * 2) / contentW, (float) (height - SCREEN_MARGIN * 2) / contentH));
		originX = (width - contentW * scale) / 2.0f;
		originY = (height - contentH * scale) / 2.0f;
		gridX = (contentW - gridW) / 2;
		gridY = headerH;

		int hoveredIndex = getOfferAt(shop, mouseX, mouseY);

		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(originX, originY);
		pose.scale(scale, scale);

		graphics.centeredText(font, title, contentW / 2, 0, 0xFFFFFFFF);
		graphics.centeredText(font, balance, contentW / 2, lineHeight + GAP, 0xFFFFFFFF);

		graphics.fill(gridX - 3, gridY - 3, gridX + gridW + 3, gridY + gridH + 3, 0x66000000);
		for (int i = 0; i < offers.size(); i++) {
			ShopClientState.Offer offer = offers.get(i);
			drawCard(graphics, offer, getCardX(i), getCardY(i), maxLines, i == hoveredIndex, canBuy(shop, offer));
		}

		pose.popMatrix();

		if (hoveredIndex >= 0) {
			ShopClientState.Offer offer = offers.get(hoveredIndex);
			graphics.setTooltipForNextFrame(font, createTooltip(offer, shop.balance() >= offer.cost(), hasTarget(shop, offer)), mouseX, mouseY);
		}
	}

	private void drawCard(GuiGraphicsExtractor graphics, ShopClientState.Offer offer, int x, int y, int maxLines, boolean hovered, boolean affordable) {
		int background = !affordable ? CARD_UNAFFORDABLE_COLOR : hovered ? CARD_HOVERED_COLOR : CARD_COLOR;
		boolean highlighted = hovered && affordable;
		// Sabotages stand out in red from what players buy for themselves
		int border = offer.target().isOtherPlayer()
				? highlighted ? SABOTAGE_BORDER_HOVERED_COLOR : SABOTAGE_BORDER_COLOR
				: highlighted ? BENEFIT_BORDER_HOVERED_COLOR : BENEFIT_BORDER_COLOR;
		drawFrame(graphics, x, y, CARD_W, cardH, background, border);

		int iconX = x + (CARD_W - ICON_SIZE) / 2;
		int iconY = y + PADDING;
		if (!offer.icon().isEmpty()) {
			Matrix3x2fStack pose = graphics.pose();
			pose.pushMatrix();
			float iconScale = ICON_SIZE / 16.0f;
			pose.translate(iconX, iconY);
			pose.scale(iconScale, iconScale);
			graphics.item(offer.icon(), 0, 0);
			pose.popMatrix();

			graphics.itemDecorations(font, offer.icon(), iconX, iconY);
		}

		int textAreaW = CARD_W - PADDING * 2;
		int textY = iconY + ICON_SIZE + ICON_TEXT_GAP;
		List<FormattedCharSequence> lines = font.split(offer.name(), textAreaW);
		int offset = (maxLines - lines.size()) * font.lineHeight / 2;
		for (int i = 0; i < lines.size(); i++) {
			FormattedCharSequence line = lines.get(i);
			graphics.text(font, line, x + (CARD_W - font.width(line)) / 2, textY + i * font.lineHeight + offset, 0xFFFFFFFF, false);
		}

		Component cost = McsrTexts.TROPICOINS.apply(offer.cost()).withStyle(affordable ? ChatFormatting.GOLD : ChatFormatting.RED);
		graphics.centeredText(font, cost, x + CARD_W / 2, textY + maxLines * font.lineHeight + ICON_TEXT_GAP, 0xFFFFFFFF);
	}

	/// Draws a row for each player the offer can be used on, then one to pick a random player and one to cancel
	private void drawTargetPicker(GuiGraphicsExtractor graphics, ShopClientState shop, ShopClientState.Offer offer, int mouseX, int mouseY) {
		List<ShopClientState.Target> targets = shop.targets();
		int lineHeight = font.lineHeight;
		rowH = Math.max(lineHeight, FACE_SIZE) + PADDING * 2;
		int rowCount = targets.size() + 2;
		int headerH = lineHeight + GAP * 2;
		int contentH = headerH + rowCount * rowH + (rowCount - 1) * GAP;
		rowsX = (width - ROW_W) / 2;
		rowsY = (height - contentH) / 2 + headerH;

		Component pickTitle = McsrTexts.PICK_TARGET.apply(offer.name().copy().withStyle(ChatFormatting.AQUA));
		graphics.centeredText(font, pickTitle, width / 2, rowsY - headerH, 0xFFFFFFFF);

		int hoveredRow = getRowAt(shop, mouseX, mouseY);
		for (int row = 0; row < rowCount; row++) {
			int y = getRowY(row);
			boolean hovered = row == hoveredRow;
			boolean playerRow = row < targets.size();
			int border = playerRow
					? hovered ? SABOTAGE_BORDER_HOVERED_COLOR : SABOTAGE_BORDER_COLOR
					: hovered ? BORDER_HOVERED_COLOR : BORDER_COLOR;
			drawFrame(graphics, rowsX, y, ROW_W, rowH, hovered ? CARD_HOVERED_COLOR : CARD_COLOR, border);

			int textY = y + (rowH - lineHeight) / 2 + 1;
			if (playerRow) {
				ShopClientState.Target target = targets.get(row);
				Component name = Component.literal("#" + (row + 1) + " ").withStyle(ChatFormatting.GRAY)
						.append(Component.literal(target.name()).withStyle(ChatFormatting.WHITE));
				Component points = McsrTexts.POINTS.apply(target.points()).withStyle(ChatFormatting.GOLD);
				int faceX = rowsX + PADDING + 2;
				PlayerFaceExtractor.extractRenderState(graphics, ClientPlayerInfo.getSkin(target.id()), faceX, y + (rowH - FACE_SIZE) / 2, FACE_SIZE);
				graphics.text(font, name, faceX + FACE_SIZE + 4, textY, 0xFFFFFFFF, false);
				graphics.text(font, points, rowsX + ROW_W - PADDING - 2 - font.width(points), textY, 0xFFFFFFFF, false);
			} else {
				Component label = row == targets.size()
						? McsrTexts.PICK_RANDOM.copy().withStyle(ChatFormatting.YELLOW)
						: McsrTexts.PICK_CANCEL.copy().withStyle(ChatFormatting.GRAY);
				graphics.centeredText(font, label, rowsX + ROW_W / 2, textY, 0xFFFFFFFF);
			}
		}
	}

	private static void drawFrame(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int background, int border) {
		graphics.fill(x, y, x + w, y + h, background);
		graphics.fill(x, y, x + w, y + 1, border);
		graphics.fill(x, y + h - 1, x + w, y + h, border);
		graphics.fill(x, y, x + 1, y + h, border);
		graphics.fill(x + w - 1, y, x + w, y + h, border);
	}

	private List<FormattedCharSequence> createTooltip(ShopClientState.Offer offer, boolean affordable, boolean hasTarget) {
		List<FormattedCharSequence> lines = new ArrayList<>(font.split(offer.name().copy().withStyle(ChatFormatting.YELLOW), TOOLTIP_WIDTH));
		offer.description().ifPresent(description -> lines.addAll(font.split(description.copy().withStyle(ChatFormatting.GRAY), TOOLTIP_WIDTH)));
		switch (offer.target()) {
			case PICK -> lines.add(McsrTexts.SABOTAGE_PICK.copy().withStyle(ChatFormatting.RED).getVisualOrderText());
			case RANDOM -> lines.add(McsrTexts.SABOTAGE_RANDOM.copy().withStyle(ChatFormatting.RED).getVisualOrderText());
			case SELF -> {
			}
		}
		lines.add(CommonComponents.EMPTY.getVisualOrderText());
		lines.add(McsrTexts.TROPICOINS.apply(offer.cost()).withStyle(ChatFormatting.GOLD).getVisualOrderText());
		if (!affordable) {
			lines.add(McsrTexts.NOT_ENOUGH_TROPICOINS.copy().withStyle(ChatFormatting.RED).getVisualOrderText());
		}
		if (!hasTarget) {
			lines.add(McsrTexts.NO_TARGETS.copy().withStyle(ChatFormatting.RED).getVisualOrderText());
		}
		return lines;
	}

	/// @return the index of the offer under the given screen position, or `-1` if there is none
	private int getOfferAt(ShopClientState shop, double mouseX, double mouseY) {
		double localX = (mouseX - originX) / scale;
		double localY = (mouseY - originY) / scale;
		for (int i = 0; i < shop.offers().size(); i++) {
			int x = getCardX(i);
			int y = getCardY(i);
			if (localX >= x && localX < x + CARD_W && localY >= y && localY < y + cardH) {
				return i;
			}
		}
		return -1;
	}

	/// @return the row of the target picker under the given screen position: the players, then random, then cancel - or `-1` if there is none
	private int getRowAt(ShopClientState shop, double mouseX, double mouseY) {
		if (mouseX < rowsX || mouseX >= rowsX + ROW_W) {
			return -1;
		}
		int rowCount = shop.targets().size() + 2;
		for (int row = 0; row < rowCount; row++) {
			int y = getRowY(row);
			if (mouseY >= y && mouseY < y + rowH) {
				return row;
			}
		}
		return -1;
	}

	private int getCardX(int index) {
		return gridX + index % columns * (CARD_W + GAP);
	}

	private int getCardY(int index) {
		return gridY + index / columns * (cardH + GAP);
	}

	private int getRowY(int row) {
		return rowsY + row * (rowH + GAP);
	}
}
