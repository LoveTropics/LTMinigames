package com.lovetropics.minigames.client.lobby.manage.screen;

import com.lovetropics.minigames.client.lobby.manage.ClientLobbyManagement;
import com.lovetropics.minigames.client.lobby.manage.screen.game_list.GameList;
import com.lovetropics.minigames.client.lobby.manage.screen.player_list.LobbyPlayerList;
import com.lovetropics.minigames.client.lobby.manage.state.ClientLobbyManageState;
import com.lovetropics.minigames.client.lobby.manage.state.ClientLobbyQueue;
import com.lovetropics.minigames.client.lobby.manage.state.ClientLobbyQueuedGame;
import com.lovetropics.minigames.client.lobby.screen.game_config.GameConfig;
import com.lovetropics.minigames.client.lobby.state.ClientGameDefinition;
import com.lovetropics.minigames.client.screen.FlexUi;
import com.lovetropics.minigames.client.screen.flex.Box;
import com.lovetropics.minigames.client.screen.flex.Layout;
import com.lovetropics.minigames.common.core.game.lobby.LobbyControls;
import com.lovetropics.minigames.common.core.game.util.GameTexts;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public final class ManageLobbyScreen extends Screen {
	private final ClientLobbyManagement.Session session;

	private ManageLobbyLayout layout;

	private EditBox nameField;
	private Button publishButton;

	private GameList gameList;
	private GameConfig gameConfig;
	private LobbyPlayerList playerList;

	private Button closeButton;

	private Button playButton;
	private Button skipButton;

	private Button restartButton;

	private int selectedGameId = -1;

	public ManageLobbyScreen(ClientLobbyManagement.Session session) {
		super(GameTexts.Ui.MANAGE_GAME_LOBBY);
		this.session = session;
	}

	@Override
	protected void init() {
		layout = new ManageLobbyLayout(this);

		ClientLobbyManageState lobby = session.lobby();

		selectedGameId = -1;
		gameList = addWidget(new GameList(this, layout.gameList, layout.leftFooter, lobby, new GameList.Handlers() {
			@Override
			public void selectQueuedGame(int queuedGameId) {
				if (queuedGameId == selectedGameId) {
					return;
				}
				selectedGameId = queuedGameId;
				gameConfig.setGame(session.lobby().getQueue().byId(queuedGameId));
			}

			@Override
			public void enqueueGame(int installedGameIndex) {
				List<ClientGameDefinition> installedGames = lobby.getInstalledGames();
				if (installedGameIndex >= 0 && installedGameIndex < installedGames.size()) {
					session.enqueueGame(installedGames.get(installedGameIndex));
				}
			}

			@Override
			public void removeQueuedGame(int queuedGameId) {
				session.removeQueuedGame(queuedGameId);
			}

			@Override
			public void reorderQueuedGame(int queuedGameId, int offset) {
				ClientLobbyQueue queue = lobby.getQueue();
				int index = queue.indexById(queuedGameId);
				if (index != -1) {
					int newIndex = Mth.clamp(index + offset, 0, queue.size() - 1);
					session.reorderQueuedGame(queuedGameId, newIndex);
				}
			}
		}));

		// TODO actually save config data
		gameConfig = addWidget(new GameConfig(this, layout.edit, () -> {
			int selectedId = selectedGameId;
			if (selectedId != -1) {
				session.configure(selectedId);
			}
		}));

		nameField = addWidget(FlexUi.createTextField(layout.name, font, GameTexts.Ui.LOBBY_NAME));
		nameField.setMaxLength(200);
		nameField.setValue(lobby.getName());

		publishButton = addRenderableWidget(FlexUi.createButton(layout.publish, GameTexts.Ui.PUBLISH, button -> {
			if (session.lobby().getVisibility().isPrivate()) {
				session.publishLobby();
			} else {
				session.focusLive();
			}
		}));

		playerList = addWidget(new LobbyPlayerList(this, lobby, layout.playerList));

		playButton = addRenderableWidget(FlexUi.createButton(layout.play, Component.literal("▶"), b -> {
			session.selectControl(LobbyControls.Type.PLAY);
		}));
		skipButton = addRenderableWidget(FlexUi.createButton(layout.skip, Component.literal("⏭"), b -> {
			session.selectControl(LobbyControls.Type.SKIP);
		}));
		restartButton = addRenderableWidget(FlexUi.createButton(layout.restart, Component.literal("⟳"), b -> {
			session.selectControl(LobbyControls.Type.RESTART);
		}));

		Component closeLobby = GameTexts.Ui.CLOSE_LOBBY.copy()
				.withStyle(ChatFormatting.RED, ChatFormatting.UNDERLINE);
		closeButton = addRenderableWidget(FlexUi.createButton(layout.close, closeLobby, b -> {
			session.closeLobby();
			onClose();
		}));
		addRenderableWidget(FlexUi.createButton(layout.done, CommonComponents.GUI_DONE, b -> onClose()));

		updateGameEntries();
		updatePublishState();
		updateControlsState();
	}

	// TODO: custom text field instance
	@Override
	public void setFocused(@Nullable GuiEventListener listener) {
		GuiEventListener lastListener = getFocused();
		if (lastListener != null && lastListener != listener) {
			onLoseFocus(lastListener);
		}
		super.setFocused(listener);
	}

	private void onLoseFocus(GuiEventListener listener) {
		if (listener == nameField) {
			applyNameField();
		}
	}

	public void updateGameEntries() {
		gameList.updateEntries();

		ClientLobbyManageState lobby = session.lobby();
		closeButton.active = lobby.getCurrentGame() == null && lobby.getQueue().isEmpty();
	}

	public void updateNameField() {
		ClientLobbyManageState lobby = session.lobby();
		nameField.setValue(lobby.getName());
	}

	public void updateControlsState() {
		ClientLobbyManageState lobby = session.lobby();
		LobbyControls.State controls = lobby.getControlsState();
		playButton.active = controls.enabled(LobbyControls.Type.PLAY);
		skipButton.active = controls.enabled(LobbyControls.Type.SKIP);
		restartButton.active = controls.enabled(LobbyControls.Type.RESTART);
	}

	public void updatePublishState() {
		ClientLobbyManageState lobby = session.lobby();
		switch (lobby.getVisibility()) {
			case PRIVATE -> {
				publishButton.setMessage(GameTexts.Ui.PUBLISH);
				publishButton.active = true;
			}
			case PUBLIC -> {
				publishButton.setMessage(GameTexts.Ui.FOCUS_LIVE);
				publishButton.active = lobby.canFocusLive();
			}
			case PUBLIC_LIVE -> {
				publishButton.setMessage(GameTexts.Ui.FOCUS_LIVE);
				publishButton.active = false;
			}
		}
	}

	@Override
	public void removed() {
		super.removed();

		session.close();
		applyNameField();
	}

	private void applyNameField() {
		String name = nameField.getValue();
		if (!name.equals(session.lobby().getName())) {
			session.setName(name);
		}
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		super.renderBackground(graphics, mouseX, mouseY, partialTicks);

		FlexUi.fill(layout.leftColumn, graphics, 0x80101010);
		FlexUi.fill(layout.rightColumn, graphics, 0x80101010);

		for (Layout marginal : layout.marginals) {
			FlexUi.fill(marginal, graphics, 0xFF101010);
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		super.render(graphics, mouseX, mouseY, partialTicks);

		gameList.render(graphics, mouseX, mouseY, partialTicks);
		gameConfig.render(graphics, mouseX, mouseY, partialTicks);

		gameList.renderOverlays(graphics, mouseX, mouseY, partialTicks);

		// TODO: make this name rendering better
		graphics.drawString(font, nameField.getMessage(), nameField.getX(), nameField.getY() - font.lineHeight - 2, CommonColors.WHITE);
		nameField.render(graphics, mouseX, mouseY, partialTicks);

		playerList.render(graphics, mouseX, mouseY);

		Box header = layout.header.content();
		graphics.drawCenteredString(font, title, header.centerX(), header.centerY(), CommonColors.WHITE);

		ClientLobbyQueue queue = session.lobby().getQueue();
		ClientLobbyQueuedGame selectedEntry = queue.byId(selectedGameId);
		if (selectedEntry != null) {
			renderSelectedGame(selectedEntry, graphics, mouseX, mouseY, partialTicks);
		}

		playerList.renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	public Optional<GuiEventListener> getChildAt(double mouseX, double mouseY) {
		return gameList.getChildAt(mouseX, mouseY).or(() -> super.getChildAt(mouseX, mouseY));
	}

	private void renderSelectedGame(ClientLobbyQueuedGame game, GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		FlexUi.fill(layout.centerHeader, graphics, 0x80101010);

		Component title = GameTexts.Ui.managingGame(game.definition());

		Box header = layout.centerHeader.content();
		graphics.drawCenteredString(font, title, header.centerX(), header.centerY() - font.lineHeight / 2, CommonColors.WHITE);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
