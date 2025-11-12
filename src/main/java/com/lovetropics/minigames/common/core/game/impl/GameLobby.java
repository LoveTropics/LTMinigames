package com.lovetropics.minigames.common.core.game.impl;

import com.google.common.collect.Lists;
import com.lovetropics.minigames.client.lobby.state.ClientCurrentGame;
import com.lovetropics.minigames.client.lobby.state.message.JoinedLobbyMessage;
import com.lovetropics.minigames.client.lobby.state.message.LeftLobbyMessage;
import com.lovetropics.minigames.client.lobby.state.message.LobbyPlayersMessage;
import com.lovetropics.minigames.client.lobby.state.message.LobbyUpdateMessage;
import com.lovetropics.minigames.common.core.game.GamePhaseType;
import com.lovetropics.minigames.common.core.game.GameResult;
import com.lovetropics.minigames.common.core.game.IGame;
import com.lovetropics.minigames.common.core.game.IGameDefinition;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.PlayerIsolation;
import com.lovetropics.minigames.common.core.game.lobby.GameLobbyMetadata;
import com.lovetropics.minigames.common.core.game.lobby.IGameLobby;
import com.lovetropics.minigames.common.core.game.lobby.ILobbyManagement;
import com.lovetropics.minigames.common.core.game.lobby.LobbyControls;
import com.lovetropics.minigames.common.core.game.lobby.LobbyStateListener;
import com.lovetropics.minigames.common.core.game.lobby.LobbyVisibility;
import com.lovetropics.minigames.common.core.game.player.PlayerIterable;
import com.lovetropics.minigames.common.core.game.player.PlayerRoleSelections;
import com.lovetropics.minigames.common.core.game.rewards.GameRewardsMap;
import com.lovetropics.minigames.common.core.game.util.GameTexts;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;

// TODO: do we want a different game lobby implementation for something like carnival games?

/**
 * This is what is created when the command /game create is run - it is not the 'waiting room' lobby, it is a game lobby, as in
 * basically a 'party' of players that will play games together.
 * <p>
 * A game lobby can have many games in its queue, each will be given a GameInstance.
 */
final class GameLobby implements IGameLobby {
	final MultiGameManager manager;
	final MinecraftServer server;
	GameLobbyMetadata metadata;

	final LobbyGameQueue gameQueue;
	final LobbyStateManager state;
	final LobbyPlayerManager players;
	final LobbyManagement management;
	final LobbyTrackingPlayers trackingPlayers;

	final LobbyStateListener stateListener = LobbyStateListener.compose(
			new NetworkUpdateListener(),
			new ChatNotifyListener()
	);
	private final GameRewardsMap rewardsMap = new GameRewardsMap();

	private boolean needsRolePrompt = false;
	private boolean closed;

	GameLobby(MultiGameManager manager, MinecraftServer server, GameLobbyMetadata metadata) {
		this.manager = manager;
		this.server = server;
		this.metadata = metadata;

		gameQueue = new LobbyGameQueue(server);
		state = new LobbyStateManager(this);
		players = new LobbyPlayerManager(this);
		management = new LobbyManagement(this);
		trackingPlayers = new LobbyTrackingPlayers(this);
	}

	@Override
	public MinecraftServer getServer() {
		return server;
	}

	@Override
	public GameLobbyMetadata getMetadata() {
		return metadata;
	}

	@Override
	public LobbyPlayerManager getPlayers() {
		return players;
	}

	@Override
	public LobbyGameQueue getGameQueue() {
		return gameQueue;
	}

	@Override
	@Nullable
	public IGame getCurrentGame() {
		GamePhase phase = getActivePhase();
		return phase != null ? phase.game : null;
	}

	@Nullable
	@Override
	public IGamePhase getTopPhase() {
		return state.getTopPhase();
	}

	@Nullable
	@Override
	public GamePhase getActivePhase() {
		GamePhase phase = state.getTopPhase();
		return phase != null ? phase.getActivePhase() : null;
	}

	@Nullable
	@Override
	public ClientCurrentGame getClientCurrentGame() {
		return state.getClientCurrentGame();
	}

	@Override
	public LobbyControls getControls() {
		return state.controls();
	}

	@Override
	public ILobbyManagement getManagement() {
		return management;
	}

	@Override
	public PlayerIterable getTrackingPlayers() {
		return trackingPlayers;
	}

	@Override
	public boolean isVisibleTo(CommandSourceStack source) {
		if (management.canManage(source)) {
			return true;
		}

		return metadata.visibility().isPublic();
	}

	void setName(String name) {
		metadata = metadata.withName(name);
		stateListener.onLobbyNameChange(this);
	}

	void setVisibility(LobbyVisibility visibility) {
		metadata = manager.setVisibility(this, visibility);
		trackingPlayers.rebuildTracking();
	}

	void tick() {
		LobbyStateManager.Change change = state.tick();
		if (change != null) {
			GameResult<Unit> result = onStateChange(change);
			if (result.isError()) {
				onStateChange(state.handleError(result.getError()));
			}
		}
	}

	private GameResult<Unit> onStateChange(LobbyStateManager.Change change) {
		GamePhase oldPhase = change.oldPhase();
		GamePhase newPhase = change.newPhase();
		if (newPhase != oldPhase) {
			GameResult<Unit> result = onGamePhaseChange(oldPhase, newPhase);
			if (result.isError()) {
				return result;
			}
		}

		management.onGameStateChange();
		stateListener.onLobbyStateChange(this);

		return GameResult.ok();
	}

	public GameRewardsMap getRewardsMap() {
		return rewardsMap;
	}

	// If old phase is null, it probably means we're entering from the main event world
	private GameResult<Unit> onGamePhaseChange(@Nullable GamePhase oldPhase, @Nullable GamePhase newPhase) {
		GameResult<Unit> result = GameResult.ok();

		if (newPhase == null && oldPhase != null) {
			onQueuePaused();
		}

		if (oldPhase != null) {
			oldPhase.destroy();
			manager.removeGamePhaseFromDimension(oldPhase.dimension(), oldPhase);
		}

		if (newPhase != null) {
			manager.addGamePhaseToDimension(newPhase.dimension(), newPhase);
			result = startPhase(newPhase);
		}

		GameInstance oldGame = oldPhase != null ? oldPhase.game : null;
		GameInstance newGame = newPhase != null ? newPhase.game : null;
		if (oldGame != newGame) {
			onGameInstanceChange(oldGame, newGame);
		}

		stateListener.onGamePhaseChange(this);

		return result;
	}

	private GameResult<Unit> startPhase(GamePhase phase) {
		return phase.start(false);
	}

	private void onGameInstanceChange(@Nullable GameInstance oldGame, @Nullable GameInstance newGame) {
		if (oldGame != null) {
			needsRolePrompt = true;
		}
		if (newGame != null) {
			onGameInstanceStart(newGame);
		}
	}

	private void onGameInstanceStart(GameInstance game) {
		IGameDefinition definition = game.definition();
		if (definition.getWaitingPhase().isPresent() && needsRolePrompt) {
			PlayerRoleSelections roleSelections = players.getRoleSelections();
			roleSelections.clearAndPromptAll(players);
		}
	}

	void onQueuePaused() {
		for (ServerPlayer player : getPlayers()) {
			onPlayerExitGame(player);
		}

		stateListener.onLobbyPaused(this);
	}

	void onPlayerLoggedIn(ServerPlayer player) {
		trackingPlayers.onPlayerLoggedIn(player);
	}

	ServerPlayer onPlayerLoggedOut(ServerPlayer player) {
		trackingPlayers.onPlayerLoggedOut(player);

		return players.logOut(player);
	}

	void onPlayerRegister(ServerPlayer player) {
		manager.addPlayerToLobby(player, this);

		stateListener.onPlayerJoin(this, player);

		GamePhase phase = state.getTopPhase();
		if (phase != null) {
			phase.onPlayerJoin(player, false);
		}

		management.onPlayersChanged();
	}

	ServerPlayer onPlayerLeave(ServerPlayer player, boolean loggingOut) {
		GamePhase phase = state.getTopPhase();
		if (phase != null) {
			player = phase.onPlayerLeave(player, loggingOut);
		}

		stateListener.onPlayerLeave(this, player);
		management.stopManaging(player);

		management.onPlayersChanged();

		manager.removePlayerFromLobby(player, this);

		rewardsMap.grant(player);

		return player;
	}

	// TODO: better abstract this logic?
	void onPlayerExitGame(ServerPlayer player) {
		rewardsMap.grant(PlayerIsolation.INSTANCE.restore(player));
	}

	void onPlayerStartTracking(ServerPlayer player) {
		stateListener.onPlayerStartTracking(this, player);
	}

	void onPlayerStopTracking(ServerPlayer player) {
		stateListener.onPlayerStopTracking(this, player);
	}

	void close(boolean serverStopping) {
		if (closed) {
			return;
		}
		closed = true;

		try {
			management.disable();
			LobbyStateManager.Change close = state.close();
			if (close != null) {
				onStateChange(close);
			}

			LobbyPlayerManager players = getPlayers();
			for (ServerPlayer player : Lists.newArrayList(players)) {
				players.remove(player, serverStopping);
			}

			stateListener.onLobbyStop(this);
			gameQueue.clear();
		} finally {
			manager.removeLobby(this);
		}
	}

	static final class ChatNotifyListener implements LobbyStateListener {
		@Override
		public void onPlayerJoin(IGameLobby lobby, ServerPlayer player) {
			IGamePhase currentPhase = lobby.getActivePhase();
			if (currentPhase != null && currentPhase.phaseType() == GamePhaseType.WAITING) {
				onPlayerJoinGame(lobby, currentPhase);
			}
		}

		@Override
		public void onPlayerLeave(IGameLobby lobby, ServerPlayer player) {
			IGamePhase currentPhase = lobby.getActivePhase();
			if (currentPhase != null && currentPhase.phaseType() == GamePhaseType.WAITING) {
				onPlayerLeaveGame(lobby, currentPhase);
			}
		}

		@Override
		public void onPlayerStartTracking(IGameLobby lobby, ServerPlayer player) {
			player.displayClientMessage(GameTexts.Status.lobbyOpened(lobby), false);
		}

		private void onPlayerJoinGame(IGameLobby lobby, IGamePhase currentPhase) {
			int minimumParticipants = currentPhase.definition().getMinimumParticipantCount();
			if (lobby.getPlayers().size() == minimumParticipants) {
				Component enoughPlayers = GameTexts.Status.enoughPlayers();
				lobby.getTrackingPlayers().sendMessage(enoughPlayers);
			}
		}

		private void onPlayerLeaveGame(IGameLobby lobby, IGamePhase currentPhase) {
			int minimumParticipants = currentPhase.definition().getMinimumParticipantCount();
			if (lobby.getPlayers().size() == minimumParticipants - 1) {
				Component noLongerEnoughPlayers = GameTexts.Status.noLongerEnoughPlayers();
				lobby.getTrackingPlayers().sendMessage(noLongerEnoughPlayers);
			}
		}

		@Override
		public void onLobbyPaused(IGameLobby lobby) {
			lobby.getPlayers().sendMessage(GameTexts.Status.lobbyPaused());
		}

		@Override
		public void onLobbyStop(IGameLobby lobby) {
			lobby.getPlayers().sendMessage(GameTexts.Status.lobbyStopped());
		}
	}

	static final class NetworkUpdateListener implements LobbyStateListener {
		@Override
		public void onPlayerJoin(IGameLobby lobby, ServerPlayer player) {
			PacketDistributor.sendToPlayer(player, JoinedLobbyMessage.create(lobby));
			lobby.getTrackingPlayers().sendPacket(LobbyPlayersMessage.update(lobby));
		}

		@Override
		public void onPlayerLeave(IGameLobby lobby, ServerPlayer player) {
			PacketDistributor.sendToPlayer(player, new LeftLobbyMessage());
			lobby.getTrackingPlayers().sendPacket(LobbyPlayersMessage.update(lobby));
		}

		@Override
		public void onPlayerStartTracking(IGameLobby lobby, ServerPlayer player) {
			PacketDistributor.sendToPlayer(player, LobbyUpdateMessage.update(lobby));
			PacketDistributor.sendToPlayer(player, LobbyPlayersMessage.update(lobby));
		}

		@Override
		public void onPlayerStopTracking(IGameLobby lobby, ServerPlayer player) {
			PacketDistributor.sendToPlayer(player, LobbyUpdateMessage.remove(lobby));
		}

		@Override
		public void onLobbyStateChange(IGameLobby lobby) {
			lobby.getTrackingPlayers().sendPacket(LobbyUpdateMessage.update(lobby));
		}

		@Override
		public void onLobbyNameChange(IGameLobby lobby) {
			lobby.getTrackingPlayers().sendPacket(LobbyUpdateMessage.update(lobby));
		}

		@Override
		public void onLobbyStop(IGameLobby lobby) {
			lobby.getTrackingPlayers().sendPacket(LobbyUpdateMessage.remove(lobby));
		}

		@Override
		public void onGamePhaseChange(IGameLobby lobby) {
			lobby.getTrackingPlayers().sendPacket(LobbyUpdateMessage.update(lobby));
		}
	}
}
