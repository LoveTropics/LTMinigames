package org.lovetropics.games.lobbies.client.manage.screen.player_list;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.player.PlayerRole;
import org.lovetropics.games.lobbies.GameLobbyTexts;
import org.lovetropics.games.lobbies.client.manage.ClientLobbyManagement;
import org.lovetropics.games.lobbies.client.manage.state.ClientOnlinePlayer;
import org.lovetropics.games.lobbies.client.screen.ConfirmLayer;
import org.lovetropics.games.lobbies.client.screen.FlexUi;
import org.lovetropics.games.lobbies.client.screen.TrimmedText;
import org.lovetropics.games.lobbies.client.screen.flex.Align;
import org.lovetropics.games.lobbies.client.screen.flex.Box;
import org.lovetropics.games.lobbies.client.screen.flex.Flex;
import org.lovetropics.games.lobbies.client.screen.flex.FlexSolver;
import org.lovetropics.games.lobbies.client.screen.flex.Layout;
import org.lovetropics.games.lobbies.client.screen.list.AbstractLTList;
import org.lovetropics.games.lobbies.client.screen.list.LTListEntry;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Lists the server's players so that a manager can add them to the lobby.
 */
public final class AddPlayerLayer extends Screen {
	private static final int PADDING = 8;
	private static final int PANEL_COLOR = 0xE0101010;

	private static final Comparator<ClientOnlinePlayer> ORDER = Comparator
			// players that are free to join first
			.comparing((ClientOnlinePlayer player) -> player.lobbyName() != null)
			.thenComparing(ClientOnlinePlayer::name, String.CASE_INSENSITIVE_ORDER);

	private final ClientLobbyManagement.Session session;

	private @Nullable List<ClientOnlinePlayer> players;
	private String search = "";

	private Box panel = new Box();
	private Box titleBox = new Box();
	private @Nullable OnlinePlayerList list;

	private AddPlayerLayer(ClientLobbyManagement.Session session) {
		super(GameLobbyTexts.Ui.ADD_PLAYER);
		this.session = session;
	}

	public static void open(ClientLobbyManagement.Session session) {
		AddPlayerLayer layer = new AddPlayerLayer(session);
		session.requestOnlinePlayers(layer::setPlayers);
		Minecraft.getInstance().gui.pushScreenLayer(layer);
	}

	private void setPlayers(List<ClientOnlinePlayer> players) {
		this.players = players.stream().sorted(ORDER).toList();
		if (list != null) {
			list.updateEntries();
		}
	}

	@Override
	protected void init() {
		Flex root = new Flex().column()
				.size(1.0F, 1.0F, Flex.Unit.PERCENT)
				.padding(PADDING * 2);

		Flex panel = root.child().column()
				.width(0.5F, Flex.Unit.PERCENT).grow(1.0F)
				.padding(PADDING)
				.alignCross(Align.Cross.CENTER);

		Flex title = panel.child()
				.width(1.0F, Flex.Unit.PERCENT).height(font.lineHeight);
		Flex search = panel.child()
				.width(1.0F, Flex.Unit.PERCENT).height(20)
				.marginTop(PADDING);
		Flex list = panel.child()
				.width(1.0F, Flex.Unit.PERCENT).grow(1.0F)
				.marginTop(PADDING / 2);
		Flex cancel = panel.child()
				.width(0.5F, Flex.Unit.PERCENT).height(20)
				.marginTop(PADDING)
				.alignCross(Align.Cross.CENTER);

		FlexSolver.Results solve = new FlexSolver(new Box(this)).apply(root);
		this.panel = solve.layout(panel).background();
		titleBox = solve.layout(title).content();

		EditBox searchField = addRenderableWidget(FlexUi.createTextField(solve.layout(search), font, GameLobbyTexts.Ui.SEARCH_PLAYERS));
		searchField.setHint(GameLobbyTexts.Ui.SEARCH_PLAYERS.copy().withStyle(ChatFormatting.DARK_GRAY));
		searchField.setValue(this.search);
		searchField.setResponder(value -> {
			this.search = value;
			if (this.list != null) {
				this.list.updateEntries();
			}
		});

		this.list = addRenderableWidget(new OnlinePlayerList(this, solve.layout(list)));
		this.list.updateEntries();

		addRenderableWidget(FlexUi.createButton(solve.layout(cancel), CommonComponents.GUI_CANCEL, button -> onClose()));

		setInitialFocus(searchField);
	}

	private void add(ClientOnlinePlayer player, PlayerRole role) {
		String lobbyName = player.lobbyName();
		if (lobbyName == null) {
			session.addPlayer(player.uuid(), role);
			onClose();
			return;
		}
		ConfirmLayer.open(
				GameLobbyTexts.Ui.confirmMovePlayerTitle(player.name()),
				GameLobbyTexts.Ui.confirmMovePlayerMessage(player.name(), lobbyName),
				() -> {
					session.addPlayer(player.uuid(), role);
					onClose();
				}
		);
	}

	@Override
	public void onClose() {
		session.stopListeningOnlinePlayers();
		minecraft.gui.popScreenLayer();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		// the manage screen below already blurred, and blurring twice in a frame crashes
		extractTransparentBackground(graphics);
		FlexUi.fill(panel, graphics, PANEL_COLOR);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);

		graphics.centeredText(font, title, titleBox.centerX(), titleBox.top(), CommonColors.WHITE);

		OnlinePlayerList list = this.list;
		if (list != null) {
			Component status = null;
			if (players == null) {
				status = GameLobbyTexts.Ui.LOADING_PLAYERS;
			} else if (list.children().isEmpty()) {
				status = GameLobbyTexts.Ui.NO_PLAYERS_TO_ADD;
			}
			if (status != null) {
				graphics.centeredText(font, status.copy().withStyle(ChatFormatting.GRAY), list.getX() + list.getWidth() / 2, list.getY() + PADDING, CommonColors.WHITE);
			}
			list.renderOverlays(graphics, mouseX, mouseY, a);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	final class OnlinePlayerList extends AbstractLTList<OnlinePlayerList.Entry> {
		OnlinePlayerList(Screen screen, Layout layout) {
			super(screen, layout, PlayerRow.HEIGHT);
		}

		@Override
		public void updateEntries() {
			clearEntries();
			List<ClientOnlinePlayer> players = AddPlayerLayer.this.players;
			if (players == null) {
				return;
			}
			String search = AddPlayerLayer.this.search.trim().toLowerCase(Locale.ROOT);
			for (ClientOnlinePlayer player : players) {
				if (search.isEmpty() || player.name().toLowerCase(Locale.ROOT).contains(search)) {
					addEntry(new Entry(this, player));
				}
			}
			setScrollAmount(0.0);
		}

		final class Entry extends LTListEntry<Entry> {
			private final ClientOnlinePlayer player;
			private final TrimmedText name;
			private final @Nullable Component lobby;

			Entry(OnlinePlayerList list, ClientOnlinePlayer player) {
				super(list, list.screen);
				this.player = player;
				name = TrimmedText.of(Component.literal(player.name()));
				lobby = player.lobbyName() != null
						? GameLobbyTexts.Ui.inLobby(player.lobbyName()).withStyle(ChatFormatting.GRAY)
						: null;
				addAction(Component.literal("⚔"), GameLobbyTexts.Ui.ADD_AS_PARTICIPANT, () -> add(player, PlayerRole.PARTICIPANT));
				addAction(Component.literal("⊙"), GameLobbyTexts.Ui.ADD_AS_SPECTATOR, () -> add(player, PlayerRole.SPECTATOR));
				setActivateAction(() -> add(player, PlayerRole.PARTICIPANT));
			}

			@Override
			public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
				PlayerRow.extract(
						graphics, font,
						getContentX(), getContentY(), getContentWidth(), getContentHeight(),
						player.uuid(), name, lobby,
						hovered, list.getSelected() == this, getVisibleActionsWidth(hovered)
				);
			}

			@Override
			public Component getNarration() {
				return Component.literal(player.name());
			}
		}
	}
}
