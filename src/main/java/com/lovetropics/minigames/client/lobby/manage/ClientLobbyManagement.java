package com.lovetropics.minigames.client.lobby.manage;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.lobby.LobbyKeybinds;
import com.lovetropics.minigames.client.lobby.ManageOrCreateLobbyPacket;
import com.lovetropics.minigames.client.lobby.manage.screen.ManageLobbyScreen;
import com.lovetropics.minigames.client.lobby.manage.state.ClientLobbyManageState;
import com.lovetropics.minigames.client.lobby.manage.state.ClientLobbyPlayer;
import com.lovetropics.minigames.client.lobby.manage.state.ClientLobbyQueue;
import com.lovetropics.minigames.client.lobby.manage.state.ClientLobbyQueuedGame;
import com.lovetropics.minigames.client.lobby.manage.state.update.ClientLobbyUpdate;
import com.lovetropics.minigames.client.lobby.manage.state.update.ServerLobbyUpdate;
import com.lovetropics.minigames.client.lobby.state.ClientCurrentGame;
import com.lovetropics.minigames.client.lobby.state.ClientGameDefinition;
import com.lovetropics.minigames.common.core.game.lobby.LobbyControls;
import com.lovetropics.minigames.common.core.game.lobby.LobbyVisibility;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import org.jspecify.annotations.Nullable;
import java.util.List;
import java.util.function.UnaryOperator;

@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public final class ClientLobbyManagement {
	private static @Nullable Session session;

	public static void update(int id, ClientLobbyUpdate.Set updates) {
		Session session = ClientLobbyManagement.session;
		if (session == null || session.id != id) {
			ClientLobbyManagement.session = session = new Session(id, new ClientLobbyManageState());
		}

		updates.applyTo(session);
	}

	@SubscribeEvent
	public static void onKeyInput(ClientTickEvent.Post event) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player != null && LobbyKeybinds.MANAGE.consumeClick()) {
			ClientPacketDistributor.sendToServer(new ManageOrCreateLobbyPacket());
		}
	}

	public static final class Session {
		final int id;
		final ClientLobbyManageState lobby;
		@Nullable ManageLobbyScreen screen;

		Session(int id, ClientLobbyManageState lobby) {
			this.id = id;
			this.lobby = lobby;
		}

		public int id() {
			return id;
		}

		public ClientLobbyManageState lobby() {
			return lobby;
		}

		public void setName(String name) {
			lobby.setName(name);
			sendUpdates(updates -> updates.setName(name));
		}

		public void enqueueGame(ClientGameDefinition game) {
			sendUpdates(updates -> updates.enqueue(game));
		}

		public void removeQueuedGame(int id) {
			sendUpdates(updates -> updates.removeQueuedGame(id));
		}

		public void reorderQueuedGame(int id, int newIndex) {
			sendUpdates(updates -> updates.reorderQueuedGame(id, newIndex));
		}

		public void selectControl(LobbyControls.Type control) {
			sendUpdates(updates -> updates.selectControl(control));
		}

		public void publishLobby() {
			LobbyVisibility visibility = lobby.getVisibility();
			if (visibility.isPrivate()) {
				lobby.setVisibility(LobbyVisibility.PUBLIC, lobby.canFocusLive());
				sendUpdates(updates -> updates.setVisibility(LobbyVisibility.PUBLIC));
			}
		}

		public void focusLive() {
			LobbyVisibility visibility = lobby.getVisibility();
			if (visibility.isPublic() && !visibility.isFocusedLive() && lobby.canFocusLive()) {
				lobby.setVisibility(LobbyVisibility.PUBLIC_LIVE, false);
				sendUpdates(updates -> updates.setVisibility(LobbyVisibility.PUBLIC_LIVE));
			}
		}

		public void closeLobby() {
			sendUpdates(ServerLobbyUpdate.Set::close);
		}

		private void sendUpdates(UnaryOperator<ServerLobbyUpdate.Set> updates) {
			ServerLobbyUpdate.Set set = ServerLobbyUpdate.Set.create();
			set = updates.apply(set);

			ServerManageLobbyMessage message = set.intoMessage(id);
			ClientPacketDistributor.sendToServer(message);
		}

		public void close() {
			ClientPacketDistributor.sendToServer(ServerManageLobbyMessage.stop(id));

			if (ClientLobbyManagement.session == this) {
				ClientLobbyManagement.session = null;
			}
		}

		public void handleInitialize(List<ClientGameDefinition> installedGames, ClientLobbyQueue queue) {
			lobby.setInstalledGames(installedGames);
			lobby.setQueue(queue);

			if (screen == null) {
				screen = new ManageLobbyScreen(this);
			}
			Minecraft.getInstance().setScreenAndShow(screen);
		}

		public void handleName(String name) {
			lobby.setName(name);
			if (screen != null) {
				screen.updateNameField();
			}
		}

		public void handleCurrentGame(@Nullable ClientCurrentGame game) {
			lobby.setCurrentGame(game);
			if (screen != null) {
				screen.updateGameEntries();
			}
		}

		public void handleQueueUpdate(IntList queue, Int2ObjectMap<ClientLobbyQueuedGame> updated) {
			lobby.updateQueue(queue, updated);
			if (screen != null) {
				screen.updateGameEntries();
			}
		}

		public void handlePlayers(List<ClientLobbyPlayer> players) {
			lobby.setPlayers(players);
		}

		public void handleControlsState(LobbyControls.State state) {
			lobby.setControlsState(state);
			if (screen != null) {
				screen.updateControlsState();
			}
		}

		public void handleVisibility(LobbyVisibility visibility, boolean canFocusLive) {
			lobby.setVisibility(visibility, canFocusLive);
			if (screen != null) {
				screen.updatePublishState();
			}
		}
	}
}
