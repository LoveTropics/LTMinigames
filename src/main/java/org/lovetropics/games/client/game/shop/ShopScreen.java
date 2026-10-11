package org.lovetropics.games.client.game.shop;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
import org.lovetropics.games.common.content.mcsr.McsrTexts;
import org.lovetropics.games.common.core.game.client_state.instance.ShopClientState;
import org.lovetropics.games.common.core.network.shop.ServerboundShopPurchasePacket;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/// Lets players spend their tropicoins on the offers of the shop
public final class ShopScreen extends Screen {
	private static final int CARD_W = 72;
	private static final int GAP = 4;
	private static final int ICON_SIZE = 20;
	private static final int PADDING = 3;
	private static final int ICON_TEXT_GAP = 2;
	private static final int MAX_COLUMNS = 5;
	private static final int SCREEN_MARGIN = 10;
	private static final int TOOLTIP_WIDTH = 200;

	private static final int CARD_COLOR = 0xAA222222;
	private static final int CARD_HOVERED_COLOR = 0xAA444444;
	private static final int CARD_UNAFFORDABLE_COLOR = 0x66000000;
	private static final int BORDER_COLOR = 0xFF555555;
	private static final int BORDER_HOVERED_COLOR = 0xFFFFFFFF;

	// Where the offers were last drawn, to know which one gets clicked
	private float scale = 1.0f;
	private float originX, originY;
	private int gridX, gridY;
	private int columns = 1;
	private int cardH;

	public ShopScreen() {
		super(McsrTexts.SHOP);
	}

	@Override
	public void tick() {
		super.tick();
		// The game is over, or we left it
		if (GameShopHandler.shop() == null) {
			onClose();
		}
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (LTKeybinds.OPEN_SHOP.matches(event)) {
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
		ShopClientState shop = GameShopHandler.shop();
		if (shop != null && event.input() == 0) {
			int index = getOfferAt(shop, event.x(), event.y());
			if (index >= 0 && shop.balance() >= shop.offers().get(index).cost()) {
				// The server checks the balance again, and sends the new one back
				ClientPacketDistributor.sendToServer(new ServerboundShopPurchasePacket(index));
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);

		ShopClientState shop = GameShopHandler.shop();
		if (shop == null || shop.offers().isEmpty()) {
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
			drawCard(graphics, offer, getCardX(i), getCardY(i), maxLines, i == hoveredIndex, shop.balance() >= offer.cost());
		}

		pose.popMatrix();

		if (hoveredIndex >= 0) {
			ShopClientState.Offer offer = offers.get(hoveredIndex);
			graphics.setTooltipForNextFrame(font, createTooltip(offer, shop.balance() >= offer.cost()), mouseX, mouseY);
		}
	}

	private void drawCard(GuiGraphicsExtractor graphics, ShopClientState.Offer offer, int x, int y, int maxLines, boolean hovered, boolean affordable) {
		int background = !affordable ? CARD_UNAFFORDABLE_COLOR : hovered ? CARD_HOVERED_COLOR : CARD_COLOR;
		int border = hovered && affordable ? BORDER_HOVERED_COLOR : BORDER_COLOR;
		graphics.fill(x, y, x + CARD_W, y + cardH, background);
		graphics.fill(x, y, x + CARD_W, y + 1, border);
		graphics.fill(x, y + cardH - 1, x + CARD_W, y + cardH, border);
		graphics.fill(x, y, x + 1, y + cardH, border);
		graphics.fill(x + CARD_W - 1, y, x + CARD_W, y + cardH, border);

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

	private List<FormattedCharSequence> createTooltip(ShopClientState.Offer offer, boolean affordable) {
		List<FormattedCharSequence> lines = new ArrayList<>(font.split(offer.name().copy().withStyle(ChatFormatting.YELLOW), TOOLTIP_WIDTH));
		offer.description().ifPresent(description -> lines.addAll(font.split(description.copy().withStyle(ChatFormatting.GRAY), TOOLTIP_WIDTH)));
		lines.add(CommonComponents.EMPTY.getVisualOrderText());
		lines.add(McsrTexts.TROPICOINS.apply(offer.cost()).withStyle(ChatFormatting.GOLD).getVisualOrderText());
		if (!affordable) {
			lines.add(McsrTexts.NOT_ENOUGH_TROPICOINS.copy().withStyle(ChatFormatting.RED).getVisualOrderText());
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

	private int getCardX(int index) {
		return gridX + index % columns * (CARD_W + GAP);
	}

	private int getCardY(int index) {
		return gridY + index / columns * (cardH + GAP);
	}
}
