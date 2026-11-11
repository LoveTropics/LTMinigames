package org.lovetropics.games.lobbies;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.config.GameConfig;
import org.lovetropics.games.common.core.game.player.MutablePlayerSet;
import org.lovetropics.games.common.core.game.player.PlayerRole;
import org.lovetropics.games.common.util.Scheduler;
import org.lovetropics.games.lobbies.client.ClientGameDefinition;
import org.lovetropics.games.lobbies.client.manage.state.ClientOnlinePlayer;
import org.lovetropics.games.lobbies.client.manage.state.update.ClientLobbyUpdate;
import org.lovetropics.games.lobbies.client.state.ClientCurrentGame;
import org.lovetropics.games.lobbies.network.ClientManageLobbyMessage;

import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

public final class LobbyManagement {
	private final GameLobby lobby;

	private final MutablePlayerSet managingPlayers = new MutablePlayerSet();

	LobbyManagement(GameLobby lobby) {
		this.lobby = lobby;
	}

	void disable() {
		managingPlayers.clear();
	}

	void onGameStateChange() {
		sendUpdates(updates -> {
			ClientCurrentGame currentGame = lobby.state.getClientCurrentGame();
			LobbyGameQueue gameQueue = lobby.getGameQueue();
			LobbyControls controls = lobby.getControls();
			// the roles shown for players depend on the game phase
			return updates.setCurrentGame(currentGame)
					.updateQueue(gameQueue)
					.setControlState(controls.asState())
					.setPlayersFrom(lobby);
		});
	}

	public boolean startManaging(ServerPlayer player) {
		if (canManage(player.createCommandSourceStack())) {
			ClientLobbyUpdate.Set initialize = ClientLobbyUpdate.Set.create()
					.setName(lobby.getMetadata().name())
					.initialize(ClientGameDefinition.collectInstalled(), lobby.getGameQueue())
					.setCurrentGame(lobby.state.getClientCurrentGame())
					.setPlayersFrom(lobby)
					.setControlState(lobby.getControls().asState())
					.setVisibility(lobby.getMetadata().visibility(), lobby.getMetadata().focusedLive(), !lobby.manager.hasFocusedLiveLobby());

			ClientManageLobbyMessage message = initialize.intoMessage(lobby.metadata.id().networkId());
			PacketDistributor.sendToPlayer(player, message);

			managingPlayers.add(player);
			return true;
		} else {
			return false;
		}
	}

	public void stopManaging(ServerPlayer player) {
		managingPlayers.remove(player);
	}

	void forceStopManaging(ServerPlayer player) {
		if (managingPlayers.remove(player)) {
			ClientLobbyUpdate.Set updates = ClientLobbyUpdate.Set.create().stopManaging();
			PacketDistributor.sendToPlayer(player, updates.intoMessage(lobby.getMetadata().id().networkId()));
		}
	}

	public boolean canManage(CommandSourceStack source) {
		return Commands.LEVEL_GAMEMASTERS.check(source.permissions()) || lobby.getMetadata().initiator().matches(source.getEntity());
	}

	public void setName(String name) {
		lobby.setName(name);
		sendUpdates(updates -> updates.setName(name));
	}

	public void enqueueGame(GameConfig game) {
		QueuedGame queued = lobby.gameQueue.enqueue(game);
		sendUpdates(updates -> updates.updateQueue(lobby.getGameQueue(), queued.networkId()));
	}

	public void removeQueuedGame(int id) {
		QueuedGame removed = lobby.gameQueue.removeByNetworkId(id);
		if (removed != null) {
			sendUpdates(updates -> updates.updateQueue(lobby.gameQueue));
		}
	}

	public void reorderQueuedGame(int id, int newIndex) {
		if (lobby.gameQueue.reorderByNetworkId(id, newIndex)) {
			sendUpdates(updates -> updates.updateQueue(lobby.gameQueue));
		}
	}

	public void playQueuedGame(int id) {
		if (!lobby.gameQueue.moveToFrontByNetworkId(id)) {
			return;
		}
		sendUpdates(updates -> updates.updateQueue(lobby.gameQueue));

		// cancelling the current game or resuming the queue both start the next queued game
		LobbyControls controls = lobby.getControls();
		LobbyControls.Action action = controls.get(LobbyControls.Type.SKIP);
		if (action == null) {
			action = controls.get(LobbyControls.Type.PLAY);
		}
		if (action != null) {
			LobbyControls.Action run = action;
			Scheduler.nextTick().run(server -> run.run());
		}
	}

	public @Nullable QueuedGame getQueuedGame(int id) {
		return lobby.gameQueue.getByNetworkId(id);
	}

	public void selectControl(LobbyControls.Type type) {
		LobbyControls.Action action = lobby.getControls().get(type);
		if (action != null) {
			Scheduler.nextTick().run(server -> {
				// TODO: handle result
				action.run();
			});
		}
	}

	public void sendOnlinePlayers() {
		List<ClientOnlinePlayer> players = lobby.getServer().getPlayerList().getPlayers().stream()
				.filter(player -> !lobby.getPlayers().contains(player.getUUID()))
				.map(player -> ClientOnlinePlayer.from(lobby.manager, player))
				.toList();
		sendUpdates(updates -> updates.setOnlinePlayers(players));
	}

	public void addPlayer(UUID id, PlayerRole role) {
		ServerPlayer player = lobby.getServer().getPlayerList().getPlayer(id);
		if (player == null) {
			return;
		}

		GameLobby currentLobby = lobby.manager.getLobbyFor(player);
		if (currentLobby == lobby) {
			return;
		}
		if (currentLobby != null) {
			currentLobby.getPlayers().remove(player, false);
			// leaving a running game replaces the player entity
			player = lobby.getServer().getPlayerList().getPlayer(id);
			if (player == null) {
				return;
			}
			player.sendSystemMessage(GameLobbyTexts.Commands.leftLobby(currentLobby));
		}

		if (lobby.getPlayers().join(player, role).isOk()) {
			player.sendSystemMessage(GameLobbyTexts.Commands.joinedLobby(lobby));
		}
	}

	public void kickPlayer(UUID id) {
		ServerPlayer player = lobby.getPlayers().getPlayerBy(id);
		if (player != null && lobby.getPlayers().remove(player, false)) {
			// leaving a running game replaces the player entity
			ServerPlayer kicked = lobby.getServer().getPlayerList().getPlayer(id);
			if (kicked != null) {
				kicked.sendSystemMessage(GameLobbyTexts.Commands.leftLobby(lobby));
			}
		}
	}

	public void setVisibility(LobbyVisibility visibility, boolean focusedLive) {
		lobby.setVisibility(visibility, focusedLive);
		sendUpdates(updates -> updates.setVisibility(visibility, focusedLive, !lobby.manager.hasFocusedLiveLobby()));
	}

	public void close() {
		lobby.close(false);
	}

	void onFocusedLiveLobbyChanged() {
		sendUpdates(updates -> updates.setVisibility(lobby.metadata.visibility(), lobby.metadata.focusedLive(), !lobby.manager.hasFocusedLiveLobby()));
	}

	void onPlayersChanged() {
		sendUpdates(updates -> updates.setPlayersFrom(lobby));
	}

	private void sendUpdates(UnaryOperator<ClientLobbyUpdate.Set> updates) {
		if (managingPlayers.isEmpty()) {
			return;
		}

		ClientLobbyUpdate.Set set = ClientLobbyUpdate.Set.create();
		set = updates.apply(set);

		ClientManageLobbyMessage message = set.intoMessage(lobby.getMetadata().id().networkId());
		managingPlayers.sendPacket(message);
	}
}
