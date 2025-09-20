package com.lovetropics.minigames.client.lobby.select_role;

import com.lovetropics.minigames.client.lobby.LeaveLobbyPacket;
import com.lovetropics.minigames.common.core.game.util.GameTexts;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class SelectPlayerRoleScreen extends Screen {
	private static final Component TITLE = GameTexts.Ui.SELECT_PLAYER_ROLE.copy()
			.withStyle(ChatFormatting.BOLD, ChatFormatting.UNDERLINE);
	private static final Component TEXT = GameTexts.Ui.selectRoleMessage(
			GameTexts.Ui.SELECT_PLAY.copy().withStyle(ChatFormatting.AQUA),
			GameTexts.Ui.SELECT_SPECTATE.copy().withStyle(ChatFormatting.AQUA)
	);

	private static final int BUTTON_WIDTH = 100;
	private static final int SPACING = 4;

	private final int lobbyId;

	private final GridLayout layout = new GridLayout().spacing(SPACING);
	private boolean responded;

	public SelectPlayerRoleScreen(int lobbyId) {
		super(TITLE);
		this.lobbyId = lobbyId;
	}

	@Override
	public Component getNarrationMessage() {
		return CommonComponents.joinForNarration(title, CommonComponents.joinForNarration(TEXT));
	}

	@Override
	protected void init() {
		GridLayout.RowHelper helper = layout.createRowHelper(1);
		layout.defaultCellSetting().alignHorizontallyCenter();

		helper.addChild(new MultiLineTextWidget(TEXT, font).setCentered(true));

		GridLayout buttons = helper.addChild(new GridLayout().spacing(SPACING), helper.newCellSettings().paddingTop(SPACING));
		GridLayout.RowHelper buttonsHelper = buttons.createRowHelper(2);
		Button playButton = buttonsHelper.addChild(Button.builder(GameTexts.Ui.SELECT_PLAY, b -> {
			sendResponse(true);
			onClose();
		}).width(BUTTON_WIDTH).build());
		buttonsHelper.addChild(Button.builder(GameTexts.Ui.SELECT_SPECTATE, b -> {
			sendResponse(false);
			onClose();
		}).width(BUTTON_WIDTH).build());

		layout.visitWidgets(this::addRenderableWidget);
		repositionElements();

		setInitialFocus(playButton);
	}

	@Override
	protected void repositionElements() {
		layout.arrangeElements();
		FrameLayout.centerInRectangle(layout, 0, 0, width, height);
	}

	private void sendResponse(boolean play) {
		if (!responded) {
			ClientPacketDistributor.sendToServer(new SelectRoleMessage(lobbyId, play));
			responded = true;
		}
	}

	@Override
	public void removed() {
		super.removed();
		sendResponse(false);
	}

	@Override
	public void onClose() {
		super.onClose();
		if (!responded) {
			ClientPacketDistributor.sendToServer(new LeaveLobbyPacket());
			responded = true;
		}
	}
}
