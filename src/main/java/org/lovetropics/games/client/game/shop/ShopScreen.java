package org.lovetropics.games.client.game.shop;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.CommonColors;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.client.LTKeybinds;
import org.lovetropics.games.client.game.handler.GameShopHandler;
import org.lovetropics.games.client.screen.ClientPlayerInfo;
import org.lovetropics.games.common.content.shop.ShopOfferFlavor;
import org.lovetropics.games.common.content.shop.ShopTarget;
import org.lovetropics.games.common.content.shop.ShopTexts;
import org.lovetropics.games.common.core.game.client_state.instance.ShopClientState;
import org.lovetropics.games.common.core.network.shop.ServerboundShopPurchasePacket;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

/// Lets players spend their tropicoins on the offers of the shop, with bonuses and sabotages in separate sections.
///
/// Offers that are used on a player of the buyer's choice first show the other players, with the most points first.
public final class ShopScreen extends Screen {
	private static final int SCROLLBAR_WIDTH = 6;
	private static final int SCROLLBAR_HEIGHT = 32;
	private static final Identifier SCROLLER_SPRITE = Identifier.withDefaultNamespace("widget/scroller");

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
	private static final int SECTION_GAP = 8;

	private static final int CARD_COLOR = 0xAA222222;
	private static final int CARD_HOVERED_COLOR = 0xAA444444;
	private static final int CARD_UNBUYABLE_OVERLAY_COLOR = 0xAA333333;
	private static final int BORDER_COLOR = 0xFF555555;
	private static final int BORDER_HOVERED_COLOR = 0xFFFFFFFF;
	private static final int BENEFIT_BORDER_COLOR = 0xFF33AA33;
	private static final int BENEFIT_BORDER_HOVERED_COLOR = 0xFF55FF55;
	private static final int SABOTAGE_BORDER_COLOR = 0xFFAA3333;
	private static final int SABOTAGE_BORDER_HOVERED_COLOR = 0xFFFF5555;

	// Where the offers were last drawn, to know which one gets clicked
	private float scale = 1.0f;
	private float originX, originY;
	private List<Section> sections = List.of();
	private int cardH;

	/// The offer the player is picking a target for, or `-1` while browsing the offers
	private int pickingOffer = -1;
	// Where the rows of the target picker were last drawn
	private int rowsX, rowsY, rowH;
	private int contentH;

	public ShopScreen() {
		super(ShopTexts.SHOP);
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
			if (pickingOffer >= 0 && pickingOffer < shop.offers().size()) {
				ShopClientState.Offer offer = shop.offers().get(pickingOffer);
				List<ShopClientState.Target> targets = offer.targetsIn(shop);
				clickTargetRow(getRowAt(event.x(), event.y(), targets), targets);
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
	private void clickTargetRow(int row, List<ShopClientState.Target> targets) {
		int offer = pickingOffer;
		pickingOffer = -1;
		// Clicking the cancel row or outside of the rows cancels
		if (row < 0 || row > targets.size()) {
			return;
		}
		// The row after the players picks one at random
		Optional<UUID> target = row < targets.size() ? Optional.of(targets.get(row).id()) : Optional.empty();
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

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (pickingOffer >= 0) {
			targetPickerScroll -= (int) Math.round(scrollY * 10.0);
			return true;
		}
		return super.mouseScrolled(x, y, scrollX, scrollY);
	}

	private static boolean canBuy(ShopClientState shop, ShopClientState.Offer offer) {
		return shop.balance().orElse(Integer.MAX_VALUE) >= offer.cost() && hasTarget(shop, offer) && !offer.isSoldOut();
	}

	private static boolean hasTarget(ShopClientState shop, ShopClientState.Offer offer) {
		return !offer.target().requiresOtherPlayer() || !offer.targetsIn(shop).isEmpty();
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
		boolean anyHasStock = offers.stream().anyMatch(offer -> offer.stock().isPresent());
		cardH = PADDING + ICON_SIZE + ICON_TEXT_GAP + maxLines * lineHeight + ICON_TEXT_GAP + lineHeight + (anyHasStock ? lineHeight : 0) + PADDING;

		List<Integer> bonuses = getOfferIndices(offers, ShopOfferFlavor.BONUS);
		List<Integer> sabotages = getOfferIndices(offers, ShopOfferFlavor.SABOTAGE);

		boolean showCost = shop.balance().isPresent();
		Component balance = showCost ? ShopTexts.TROPICOINS.apply(shop.balance().get()).withStyle(ChatFormatting.GOLD) : null;
		int contentW = Math.max(Math.max(getColumns(bonuses), getColumns(sabotages)) * (CARD_W + GAP) - GAP, Math.max(font.width(title), balance != null ? font.width(balance) : 0));

		List<Section> newSections = new ArrayList<>();
		int contentH = lineHeight * 2 + GAP;
		contentH = addSection(newSections, ShopTexts.BONUSES.copy().withStyle(ChatFormatting.GREEN), bonuses, contentW, contentH);
		contentH = addSection(newSections, ShopTexts.SABOTAGES.copy().withStyle(ChatFormatting.RED), sabotages, contentW, contentH);
		sections = newSections;

		// Shrink the shop on small screens rather than cutting it off
		scale = Math.min(1.0f, Math.min((float) (width - SCREEN_MARGIN * 2) / contentW, (float) (height - SCREEN_MARGIN * 2) / contentH));
		originX = (width - contentW * scale) / 2.0f;
		originY = (height - contentH * scale) / 2.0f;

		int hoveredIndex = getOfferAt(shop, mouseX, mouseY);

		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(originX, originY);
		pose.scale(scale, scale);

		graphics.centeredText(font, title, contentW / 2, 0, 0xFFFFFFFF);
		if (balance != null) {
			graphics.centeredText(font, balance, contentW / 2, lineHeight + GAP, 0xFFFFFFFF);
		}

		for (Section section : sections) {
			graphics.centeredText(font, section.title(), contentW / 2, section.titleY(), 0xFFFFFFFF);
			graphics.fill(section.gridX() - 3, section.gridY() - 3, section.gridX() + section.gridW() + 3, section.gridY() + section.gridH(cardH) + 3, 0x66000000);
			for (int position = 0; position < section.offers().size(); position++) {
				int index = section.offers().get(position);
				drawCard(graphics, shop, offers.get(index), section.getCardX(position), section.getCardY(position, cardH), maxLines, index == hoveredIndex, showCost);
			}
		}

		pose.popMatrix();

		if (hoveredIndex >= 0) {
			ShopClientState.Offer offer = offers.get(hoveredIndex);
			graphics.setTooltipForNextFrame(font, createTooltip(offer, shop.balance().orElse(Integer.MAX_VALUE) >= offer.cost(), hasTarget(shop, offer), showCost), mouseX, mouseY);
		}
	}

	private static List<Integer> getOfferIndices(List<ShopClientState.Offer> offers, ShopOfferFlavor flavor) {
		return IntStream.range(0, offers.size())
				.filter(i -> offers.get(i).flavor() == flavor)
				.boxed().toList();
	}

	private static int getColumns(List<Integer> offers) {
		return Math.min(MAX_COLUMNS, offers.size());
	}

	private int addSection(List<Section> sections, Component title, List<Integer> offers, int contentW, int y) {
		if (offers.isEmpty()) {
			return y;
		}
		int columns = getColumns(offers);
		int titleY = y + SECTION_GAP;
		int gridW = columns * (CARD_W + GAP) - GAP;
		Section section = new Section(title, offers, columns, titleY, (contentW - gridW) / 2, titleY + font.lineHeight + GAP);
		sections.add(section);
		return section.gridY() + section.gridH(cardH);
	}

	private void drawCard(GuiGraphicsExtractor graphics, ShopClientState shop, ShopClientState.Offer offer, int x, int y, int maxLines, boolean hovered, boolean showCost) {
		boolean buyable = canBuy(shop, offer);
		boolean highlighted = hovered && buyable;
		int background = highlighted ? CARD_HOVERED_COLOR : CARD_COLOR;
		int border = !buyable || offer.flavor() == ShopOfferFlavor.SABOTAGE
				? highlighted ? BORDER_HOVERED_COLOR : BORDER_COLOR
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

		// Grays out everything drawn so far, so that only the price keeps its color
		if (!buyable) {
			graphics.fill(x, y, x + CARD_W, y + cardH, CARD_UNBUYABLE_OVERLAY_COLOR);
		}

		int costY = textY + maxLines * font.lineHeight + ICON_TEXT_GAP;
		if (showCost) {
			Component cost = ShopTexts.TROPICOINS.apply(offer.cost()).withStyle(shop.balance().orElse(Integer.MAX_VALUE) >= offer.cost() ? ChatFormatting.GOLD : ChatFormatting.RED);
			graphics.centeredText(font, cost, x + CARD_W / 2, costY, 0xFFFFFFFF);
		}
		Component stock = getStockText(offer);
		if (stock != null) {
			graphics.centeredText(font, stock, x + CARD_W / 2, costY + font.lineHeight, 0xFFFFFFFF);
		}
	}

	private static @Nullable Component getStockText(ShopClientState.Offer offer) {
		if (offer.stock().isEmpty()) {
			return null;
		}
		return offer.isSoldOut()
				? ShopTexts.SOLD_OUT.copy().withStyle(ChatFormatting.RED)
				: ShopTexts.STOCK_LEFT.apply(offer.stock().get()).withStyle(ChatFormatting.GRAY);
	}

	private int targetPickerScroll;

	/// Draws a row for each player the offer can be used on, then one to pick a random player and one to cancel
	private void drawTargetPicker(GuiGraphicsExtractor graphics, ShopClientState shop, ShopClientState.Offer offer, int mouseX, int mouseY) {
		List<ShopClientState.Target> targets = offer.targetsIn(shop);
		int lineHeight = font.lineHeight;
		rowH = Math.max(lineHeight, FACE_SIZE) + PADDING * 2;
		int rowCount = targets.size() + 2;
		int headerH = lineHeight + GAP * 2;

		int maxContentH = graphics.guiHeight() - 40 - headerH;
		int actualContentH = rowCount * rowH + (rowCount - 1) * GAP;
		contentH = Math.min(actualContentH, maxContentH);

		rowsX = (width - ROW_W) / 2;
		rowsY = (height - contentH + headerH) / 2;

		int maxScrollAmount = Math.max(actualContentH - maxContentH, 0);

		Component pickTitle = ShopTexts.PICK_TARGET.apply(offer.name().copy().withStyle(ChatFormatting.AQUA));
		graphics.centeredText(font, pickTitle, width / 2, rowsY - headerH, 0xFFFFFFFF);

		// They're calling it the most scuffed scrollbar of all time
		targetPickerScroll = Mth.clamp(targetPickerScroll, 0, maxScrollAmount);

		if (maxScrollAmount > 0) {
			int offset = targetPickerScroll * (contentH - SCROLLBAR_HEIGHT) / maxScrollAmount;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_SPRITE, rowsX + ROW_W + PADDING, rowsY + offset, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT);
		}

		graphics.enableScissor(rowsX, rowsY, rowsX + ROW_W, rowsY + contentH);

		int hoveredRow = getRowAt(mouseX, mouseY, offer.targetsIn(shop));
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
				Component name = Component.empty()
						.append(Component.literal("#" + (row + 1) + " ").withStyle(ChatFormatting.GRAY))
						.append(target.name());
				Component points = ShopTexts.POINTS.apply(target.points()).withStyle(ChatFormatting.GOLD);
				int leftX = rowsX + PADDING + 2;
				if (!offer.targetTeam()) {
					PlayerFaceExtractor.extractRenderState(graphics, ClientPlayerInfo.getSkin(target.id()), leftX, y + (rowH - FACE_SIZE) / 2, FACE_SIZE);
					graphics.text(font, name, leftX + FACE_SIZE + 4, textY, CommonColors.WHITE, false);
					graphics.text(font, points, rowsX + ROW_W - PADDING - 2 - font.width(points), textY, CommonColors.WHITE, false);
				} else {
					graphics.text(font, name, leftX, textY, CommonColors.WHITE, false);
					graphics.text(font, points, rowsX + ROW_W - PADDING - 2 - font.width(points), textY, CommonColors.WHITE, false);
				}
			} else {
				Component label = row == targets.size()
						? ShopTexts.PICK_RANDOM.copy().withStyle(ChatFormatting.YELLOW)
						: ShopTexts.PICK_CANCEL.copy().withStyle(ChatFormatting.GRAY);
				graphics.centeredText(font, label, rowsX + ROW_W / 2, textY, CommonColors.WHITE);
			}
		}

		graphics.disableScissor();
	}

	public static void drawFrame(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int background, int border) {
		graphics.fill(x, y, x + w, y + h, background);
		graphics.fill(x, y, x + w, y + 1, border);
		graphics.fill(x, y + h - 1, x + w, y + h, border);
		graphics.fill(x, y, x + 1, y + h, border);
		graphics.fill(x + w - 1, y, x + w, y + h, border);
	}

	private List<FormattedCharSequence> createTooltip(ShopClientState.Offer offer, boolean affordable, boolean hasTarget, boolean showCost) {
		List<FormattedCharSequence> lines = new ArrayList<>(font.split(ComponentUtils.mergeStyles(offer.name(), Style.EMPTY.withColor(ChatFormatting.YELLOW)), TOOLTIP_WIDTH));
		for (Component line : offer.description()) {
			lines.addAll(font.split(ComponentUtils.mergeStyles(line, Style.EMPTY.withColor(ChatFormatting.GRAY)), TOOLTIP_WIDTH));
		}
		switch (offer.flavor()) {
			case BONUS -> lines.add(ShopTexts.BONUS.getVisualOrderText());
			case SABOTAGE ->  lines.add(ShopTexts.SABOTAGE.getVisualOrderText());
		}
		switch (offer.target()) {
			case PICK -> lines.add(ShopTexts.TARGET_PICK.getVisualOrderText());
			case RANDOM -> lines.add(ShopTexts.TARGET_RANDOM.getVisualOrderText());
			case SELF -> {
			}
		}
		lines.add(CommonComponents.EMPTY.getVisualOrderText());
		if (showCost) {
			lines.add(ShopTexts.TROPICOINS.apply(offer.cost()).withStyle(ChatFormatting.GOLD).getVisualOrderText());
		}
		Component stock = getStockText(offer);
		if (stock != null) {
			lines.add(stock.getVisualOrderText());
		}
		if (!affordable) {
			lines.add(ShopTexts.NOT_ENOUGH_TROPICOINS.copy().withStyle(ChatFormatting.RED).getVisualOrderText());
		}
		if (!hasTarget) {
			lines.add(ShopTexts.NO_TARGETS.copy().withStyle(ChatFormatting.RED).getVisualOrderText());
		}
		return lines;
	}

	/// @return the index of the offer under the given screen position, or `-1` if there is none
	private int getOfferAt(ShopClientState shop, double mouseX, double mouseY) {
		double localX = (mouseX - originX) / scale;
		double localY = (mouseY - originY) / scale;
		for (Section section : sections) {
			for (int position = 0; position < section.offers().size(); position++) {
				int index = section.offers().get(position);
				int x = section.getCardX(position);
				int y = section.getCardY(position, cardH);
				// The offers may have changed since they were last drawn
				if (index < shop.offers().size() && localX >= x && localX < x + CARD_W && localY >= y && localY < y + cardH) {
					return index;
				}
			}
		}
		return -1;
	}

	/// @return the row of the target picker under the given screen position: the players, then random, then cancel - or `-1` if there is none
	private int getRowAt(double mouseX, double mouseY, List<ShopClientState.Target> targets) {
		if (mouseX < rowsX || mouseX >= rowsX + ROW_W) {
			return -1;
		}
		if (mouseY < rowsY || mouseY >= rowsY + contentH) {
			return -1;
		}
		int rowCount = targets.size() + 2;
		for (int row = 0; row < rowCount; row++) {
			int y = getRowY(row);
			if (mouseY >= y && mouseY < y + rowH) {
				return row;
			}
		}
		return -1;
	}

	private int getRowY(int row) {
		return rowsY + row * (rowH + GAP) - targetPickerScroll;
	}

	/// @param offers the indices of the offers in this section
	private record Section(Component title, List<Integer> offers, int columns, int titleY, int gridX, int gridY) {
		int gridW() {
			return columns * (CARD_W + GAP) - GAP;
		}

		int gridH(int cardH) {
			int rows = (offers.size() + columns - 1) / columns;
			return rows * (cardH + GAP) - GAP;
		}

		int getCardX(int position) {
			return gridX + position % columns * (CARD_W + GAP);
		}

		int getCardY(int position, int cardH) {
			return gridY + position / columns * (cardH + GAP);
		}
	}
}
