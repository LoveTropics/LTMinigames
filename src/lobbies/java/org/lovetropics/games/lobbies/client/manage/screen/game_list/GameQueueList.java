package org.lovetropics.games.lobbies.client.manage.screen.game_list;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.lobbies.GameLobbyTexts;
import org.lovetropics.games.lobbies.client.manage.state.ClientLobbyManageState;
import org.lovetropics.games.lobbies.client.manage.state.ClientLobbyQueue;
import org.lovetropics.games.lobbies.client.screen.flex.Layout;
import org.lovetropics.games.lobbies.client.state.ClientCurrentGame;

public final class GameQueueList extends AbstractGameList {
	private static final Component TITLE = GameLobbyTexts.Ui.GAME_QUEUE.copy()
			.withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD);
	private static final Component ADD_GAME = Component.literal("+ ").append(GameLobbyTexts.Ui.ADD_GAME)
			.withStyle(ChatFormatting.GRAY);

	private static final int NO_GAME_ID = -1;

	private final ClientLobbyManageState lobby;

	private final Handlers handlers;

	public GameQueueList(Screen screen, Layout main, ClientLobbyManageState lobby, Handlers handlers) {
		super(screen, main, TITLE);
		this.lobby = lobby;
		this.handlers = handlers;
	}

	@Override
	public void updateEntries() {
		int selectedId = getSelected() != null ? getSelected().getId() : NO_GAME_ID;

		clearEntries();

		addEntry(createCurrentGameEntry(lobby.getCurrentGame()));

		for (ClientLobbyQueue.Entry entry : lobby.getQueue().entries()) {
			Entry listEntry = createQueuedEntry(entry);
			addEntry(listEntry);

			if (listEntry.getId() == selectedId) {
				setSelected(listEntry);
			}
		}

		addEntry(createAddGameEntry());
	}

	private Entry createQueuedEntry(ClientLobbyQueue.Entry entry) {
		int id = entry.id();
		Entry listEntry = Entry.game(this, id, entry.game().definition())
				.setDraggable(offset -> handlers.reorder(id, offset));
		listEntry.addAction(Component.literal("❌"), GameLobbyTexts.Ui.REMOVE_FROM_QUEUE, () -> remove(listEntry));
		return listEntry;
	}

	private Entry createAddGameEntry() {
		Entry entry = new Entry(this, NO_GAME_ID)
				.setTitle(ADD_GAME)
				.setCentered(true);
		entry.setActivateAction(handlers::enqueue);
		entry.setActivateOnClick(true);
		return entry;
	}

	private Entry createCurrentGameEntry(@Nullable ClientCurrentGame game) {
		Entry entry = new Entry(this, NO_GAME_ID)
				.setBanner(true);

		if (game != null) {
			applyRunningGame(game, entry);
		} else {
			applyInactiveGame(entry);
		}

		return entry;
	}

	private void applyRunningGame(ClientCurrentGame game, Entry entry) {
		MutableComponent gameName = game.definition().name().copy().withStyle(ChatFormatting.UNDERLINE);
		entry.setTitle(Component.literal("▶ ").append(gameName));

		if (game.error().isPresent()) {
			entry.setSubtitle(Component.literal("⚠ ").append(game.error().get().copy().withStyle(ChatFormatting.RED)));

			entry.setBackgroundColor(0xFF201010)
					.setHoveredColor(0xFF402020)
					.setSelectedColor(0xFF402020)
					.setOutlineColor(0xFF804040);
		} else {
			entry.setBackgroundColor(0xFF102010)
					.setHoveredColor(0xFF204020)
					.setSelectedColor(0xFF204020)
					.setOutlineColor(0xFF408040);
		}
	}

	private void applyInactiveGame(Entry entry) {
		MutableComponent inactive = GameLobbyTexts.Ui.GAME_INACTIVE.copy().withStyle(ChatFormatting.UNDERLINE);
		entry.setTitle(Component.literal("⏸ ").append(inactive));

		entry.setBackgroundColor(0xFF202010)
				.setHoveredColor(0xFF404020)
				.setSelectedColor(0xFF404020)
				.setOutlineColor(0xFF808040);
	}

	private void remove(Entry entry) {
		if (getSelected() == entry) {
			setSelected(null);
		}
		removeEntry(entry);
		handlers.remove(entry.getId());
	}

	@Override
	public void setSelected(@Nullable Entry entry) {
		handlers.select(entry != null ? entry.getId() : NO_GAME_ID);
		super.setSelected(entry);
	}

	public interface Handlers {
		void select(int id);

		void enqueue();

		void remove(int id);

		void reorder(int id, int offset);
	}
}
