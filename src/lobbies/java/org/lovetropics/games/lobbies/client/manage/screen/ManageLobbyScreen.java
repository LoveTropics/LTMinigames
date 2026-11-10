package org.lovetropics.games.lobbies.client.manage.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.lobbies.GameLobbyTexts;
import org.lovetropics.games.lobbies.LobbyControls;
import org.lovetropics.games.lobbies.client.ClientGameDefinition;
import org.lovetropics.games.lobbies.client.manage.ClientLobbyManagement;
import org.lovetropics.games.lobbies.client.manage.screen.game_list.GameList;
import org.lovetropics.games.lobbies.client.manage.screen.player_list.LobbyPlayerList;
import org.lovetropics.games.lobbies.client.manage.state.ClientLobbyManageState;
import org.lovetropics.games.lobbies.client.manage.state.ClientLobbyQueue;
import org.lovetropics.games.lobbies.client.screen.ConfirmLayer;
import org.lovetropics.games.lobbies.client.screen.FlexUi;
import org.lovetropics.games.lobbies.client.screen.flex.Box;
import org.lovetropics.games.lobbies.client.screen.flex.Layout;
import org.lovetropics.games.lobbies.client.state.ClientCurrentGame;

import java.util.List;
import java.util.Optional;

public final class ManageLobbyScreen extends Screen {
	private final ClientLobbyManagement.Session session;

	private ManageLobbyLayout layout;

	private EditBox nameField;
	private Button publishButton;
	private Button focusLiveButton;

	private GameList gameList;
	private LobbyPlayerList playerList;

	private Button closeButton;


	private int selectedGameId = -1;

	public ManageLobbyScreen(ClientLobbyManagement.Session session) {
		super(GameLobbyTexts.Ui.MANAGE_GAME_LOBBY);
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
			public void playQueuedGame(int queuedGameId) {
				int index = lobby.getQueue().indexById(queuedGameId);
				if (index == -1) {
					return;
				}
				ClientCurrentGame currentGame = lobby.getCurrentGame();
				boolean stopsGame = currentGame != null && currentGame.error().isEmpty();
				if (index == 0 && !stopsGame) {
					session.playQueuedGame(queuedGameId);
				} else {
					ConfirmLayer.open(
							GameLobbyTexts.Ui.CONFIRM_PLAY_NOW_TITLE,
							GameLobbyTexts.Ui.confirmPlayNowMessage(stopsGame, index > 0),
							() -> session.playQueuedGame(queuedGameId)
					);
				}
			}

			@Override
			public void selectControl(LobbyControls.Type control) {
				switch (control) {
					case STOP -> ConfirmLayer.open(
							GameLobbyTexts.Ui.CONFIRM_STOP_GAME_TITLE,
							GameLobbyTexts.Ui.CONFIRM_STOP_GAME_MESSAGE,
							() -> session.selectControl(control)
					);
					case RESTART -> ConfirmLayer.open(
							GameLobbyTexts.Ui.CONFIRM_RESTART_GAME_TITLE,
							GameLobbyTexts.Ui.CONFIRM_RESTART_GAME_MESSAGE,
							() -> session.selectControl(control)
					);
					default -> session.selectControl(control);
				}
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

		nameField = addWidget(FlexUi.createTextField(layout.name, font, GameLobbyTexts.Ui.LOBBY_NAME));
		nameField.setMaxLength(200);
		nameField.setValue(lobby.getName());

		publishButton = addRenderableWidget(FlexUi.createButton(layout.publish, GameLobbyTexts.Ui.PUBLISH, button -> session.toggleVisibility()));
		focusLiveButton = addRenderableWidget(FlexUi.createButton(layout.focusLive, GameLobbyTexts.Ui.FOCUS_LIVE, button -> ConfirmLayer.open(
				GameLobbyTexts.Ui.CONFIRM_FOCUS_LIVE_TITLE,
				GameLobbyTexts.Ui.CONFIRM_FOCUS_LIVE_MESSAGE,
				session::focusLive
		)));

		Component closeLobby = GameLobbyTexts.Ui.CLOSE_LOBBY.copy()
				.withStyle(ChatFormatting.RED, ChatFormatting.UNDERLINE);
		closeButton = addRenderableWidget(FlexUi.createButton(layout.close, closeLobby, button -> {
			if (lobby.getQueue().isEmpty()) {
				closeLobby();
			} else {
				ConfirmLayer.open(
						GameLobbyTexts.Ui.CONFIRM_CLOSE_LOBBY_TITLE,
						GameLobbyTexts.Ui.CONFIRM_CLOSE_LOBBY_MESSAGE,
						this::closeLobby
				);
			}
		}));

		playerList = addWidget(new LobbyPlayerList(this, lobby, layout.playerList));

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

	private void closeLobby() {
		session.closeLobby();
		onClose();
	}

	public void updateGameEntries() {
		gameList.updateEntries();

		ClientLobbyManageState lobby = session.lobby();
		closeButton.active = lobby.getCurrentGame() == null;
	}

	public void updateNameField() {
		ClientLobbyManageState lobby = session.lobby();
		nameField.setValue(lobby.getName());
	}

	public void updateControlsState() {
		// inline control actions depend on the controls
		gameList.updateEntries();
	}

	public void updatePublishState() {
		ClientLobbyManageState lobby = session.lobby();
		switch (lobby.getVisibility()) {
			case PRIVATE -> publishButton.setMessage(GameLobbyTexts.Ui.PUBLISH);
			case PUBLIC -> publishButton.setMessage(GameLobbyTexts.Ui.UNPUBLISH);
		}
		focusLiveButton.active = lobby.canFocusLive() && !lobby.isFocusedLive();
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
	protected void extractMenuBackground(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
		super.extractMenuBackground(graphics, x, y, width, height);

		for (Layout marginal : layout.marginals) {
			FlexUi.fill(marginal, graphics, 0xFF101010);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTicks);

		gameList.extractRenderState(graphics, mouseX, mouseY, partialTicks);

		gameList.renderOverlays(graphics, mouseX, mouseY, partialTicks);

		// TODO: make this name rendering better
		graphics.text(font, nameField.getMessage(), nameField.getX(), nameField.getY() - font.lineHeight - 2, CommonColors.WHITE);
		nameField.extractRenderState(graphics, mouseX, mouseY, partialTicks);

		playerList.render(graphics, mouseX, mouseY);

		Box header = layout.header.content();
		graphics.centeredText(font, title, header.centerX(), header.centerY(), CommonColors.WHITE);

		playerList.renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	public Optional<GuiEventListener> getChildAt(double mouseX, double mouseY) {
		return gameList.getChildAt(mouseX, mouseY).or(() -> super.getChildAt(mouseX, mouseY));
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.isEscape() && gameList.back()) {
			setFocused(gameList);
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
