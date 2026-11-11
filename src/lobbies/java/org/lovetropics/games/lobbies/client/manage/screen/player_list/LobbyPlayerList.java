package org.lovetropics.games.lobbies.client.manage.screen.player_list;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.lobbies.GameLobbyTexts;
import org.lovetropics.games.lobbies.client.manage.state.ClientLobbyManageState;
import org.lovetropics.games.lobbies.client.manage.state.ClientLobbyPlayer;
import org.lovetropics.games.lobbies.client.screen.TrimmedText;
import org.lovetropics.games.lobbies.client.screen.flex.Layout;
import org.lovetropics.games.lobbies.client.screen.list.AbstractLTList;
import org.lovetropics.games.lobbies.client.screen.list.LTListEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class LobbyPlayerList extends AbstractLTList<LobbyPlayerList.Entry> {
	private static final Comparator<ClientLobbyPlayer> ORDER = Comparator.comparing(ClientLobbyPlayer::name, String.CASE_INSENSITIVE_ORDER);

	private final ClientLobbyManageState lobby;
	private final Consumer<UUID> kick;

	public LobbyPlayerList(Screen screen, ClientLobbyManageState lobby, Layout layout, Consumer<UUID> kick) {
		super(screen, layout, PlayerRow.HEIGHT);
		this.lobby = lobby;
		this.kick = kick;
	}

	@Override
	public void updateEntries() {
		Entry selected = getSelected();
		UUID selectedId = selected != null ? selected.player.uuid() : null;

		clearEntries();

		List<ClientLobbyPlayer> players = new ArrayList<>(lobby.getPlayers());
		players.sort(ORDER);

		for (ClientLobbyPlayer player : players) {
			Entry entry = new Entry(this, player);
			addEntry(entry);
			if (player.uuid().equals(selectedId)) {
				setSelected(entry);
			}
		}
	}

	public static final class Entry extends LTListEntry<Entry> {
		final ClientLobbyPlayer player;
		private final TrimmedText name;
		private final @Nullable Component playingRole;

		Entry(LobbyPlayerList list, ClientLobbyPlayer player) {
			super(list, list.screen);
			this.player = player;
			name = TrimmedText.of(Component.literal(player.name()));
			playingRole = player.playingRole() != null
					? GameLobbyTexts.Ui.roleDescription(player.playingRole()).copy().withStyle(ChatFormatting.GRAY)
					: null;
			addAction(Component.literal("❌"), GameLobbyTexts.Ui.KICK_PLAYER, () -> list.kick.accept(player.uuid()));
		}

		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
			PlayerRow.extract(
					graphics, screen.getMinecraft().font,
					getContentX(), getContentY(), getContentWidth(), getContentHeight(),
					player.uuid(), name, playingRole,
					hovered, list.getSelected() == this, getVisibleActionsWidth(hovered)
			);
		}

		@Override
		public Component getNarration() {
			return CommonComponents.EMPTY;
		}
	}
}
