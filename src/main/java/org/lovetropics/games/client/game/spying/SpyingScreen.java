package org.lovetropics.games.client.game.spying;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.client.game.shop.ShopScreen;
import org.lovetropics.games.client.screen.ClientPlayerInfo;
import org.lovetropics.games.common.content.shop.ShopTexts;
import org.lovetropics.games.common.content.tropic_rush.TropicRushTexts;
import org.lovetropics.games.common.core.game.client_state.instance.ShopClientState;
import org.lovetropics.games.common.core.network.spying.ClientboundOpenSpyingScreenPacket;
import org.lovetropics.games.common.core.network.spying.ServerboundSpyPacket;

import java.util.List;
import java.util.stream.Stream;

public final class SpyingScreen extends Screen {
	private static final int ROW_W = 160;
	private static final int GAP = 4;
	private static final int PADDING = 3;
	private static final int FACE_SIZE = 10;

	private static final int ROW_COLOR = 0xAA222222;
	private static final int ROW_HOVERED_COLOR = 0xAA444444;
	private static final int BORDER_COLOR = 0xFF555555;
	private static final int BORDER_HOVERED_COLOR = 0xFFFFFFFF;
	private static final int SPYING_BORDER_COLOR = 0xFF8833AA;
	private static final int SPYING_BORDER_HOVERED_COLOR = 0xFFCC66FF;

	private final List<ShopClientState.Target> targets;
	private ShopClientState.@Nullable Target target;

	private int rowsX, rowsY, rowH;

	private SpyingScreen(List<ShopClientState.Target> targets) {
		super(TropicRushTexts.SPYING_PICK_TARGET);
		this.targets = targets;
	}

	public static void open(ClientboundOpenSpyingScreenPacket packet, IPayloadContext context) {
		Minecraft.getInstance().setScreenAndShow(new SpyingScreen(packet.targets()));
	}

	private List<Component> getRowLabels() {
		if (target != null) {
			return List.of(TropicRushTexts.SPYING_SEE_INVENTORY, TropicRushTexts.SPYING_WATCH, CommonComponents.GUI_BACK);
		}
		return Stream.concat(
				targets.stream().map(target -> (Component) Component.literal(target.name())),
				Stream.of(CommonComponents.GUI_CANCEL)
		).toList();
	}

	@Override
	public void onClose() {
		if (target != null) {
			target = null;
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
		if (event.input() != 0) {
			return super.mouseClicked(event, doubleClick);
		}
		int row = getRowAt(event.x(), event.y());
		int lastRow = getRowLabels().size() - 1;
		if (row < 0) {
			return super.mouseClicked(event, doubleClick);
		}
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
		if (row == lastRow) {
			onClose();
		} else if (target == null) {
			target = targets.get(row);
		} else {
			ClientPacketDistributor.sendToServer(new ServerboundSpyPacket(target.id(), row == 1));
			super.onClose();
		}
		return true;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);

		List<Component> labels = getRowLabels();
		int lineHeight = font.lineHeight;
		rowH = Math.max(lineHeight, FACE_SIZE) + PADDING * 2;
		int headerH = lineHeight + GAP * 2;
		int contentH = headerH + labels.size() * rowH + (labels.size() - 1) * GAP;
		rowsX = (width - ROW_W) / 2;
		rowsY = (height - contentH) / 2 + headerH;

		Component header = target != null ? TropicRushTexts.SPYING_PICK_ACTION.apply(Component.literal(target.name()).withStyle(ChatFormatting.LIGHT_PURPLE)) : title;
		graphics.centeredText(font, header, width / 2, rowsY - headerH, 0xFFFFFFFF);

		int hoveredRow = getRowAt(mouseX, mouseY);
		for (int row = 0; row < labels.size(); row++) {
			int y = getRowY(row);
			boolean hovered = row == hoveredRow;
			boolean last = row == labels.size() - 1;
			int border = last
					? hovered ? BORDER_HOVERED_COLOR : BORDER_COLOR
					: hovered ? SPYING_BORDER_HOVERED_COLOR : SPYING_BORDER_COLOR;
			ShopScreen.drawFrame(graphics, rowsX, y, ROW_W, rowH, hovered ? ROW_HOVERED_COLOR : ROW_COLOR, border);

			int textY = y + (rowH - lineHeight) / 2 + 1;
			if (target == null && !last) {
				ShopClientState.Target player = targets.get(row);
				int faceX = rowsX + PADDING + 2;
				PlayerFaceExtractor.extractRenderState(graphics, ClientPlayerInfo.getSkin(player.id()), faceX, y + (rowH - FACE_SIZE) / 2, FACE_SIZE);
				graphics.text(font, labels.get(row), faceX + FACE_SIZE + 4, textY, 0xFFFFFFFF, false);
				Component points = ShopTexts.POINTS.apply(player.points()).withStyle(ChatFormatting.GOLD);
				graphics.text(font, points, rowsX + ROW_W - PADDING - 2 - font.width(points), textY, 0xFFFFFFFF, false);
			} else {
				graphics.centeredText(font, labels.get(row), rowsX + ROW_W / 2, textY, last ? 0xFFAAAAAA : 0xFFFFFFFF);
			}
		}
	}

	private int getRowAt(double mouseX, double mouseY) {
		if (mouseX < rowsX || mouseX >= rowsX + ROW_W) {
			return -1;
		}
		for (int row = 0; row < getRowLabels().size(); row++) {
			int y = getRowY(row);
			if (mouseY >= y && mouseY < y + rowH) {
				return row;
			}
		}
		return -1;
	}

	private int getRowY(int row) {
		return rowsY + row * (rowH + GAP);
	}
}
