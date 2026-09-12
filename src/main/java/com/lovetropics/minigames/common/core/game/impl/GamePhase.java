package com.lovetropics.minigames.common.core.game.impl;

import com.google.common.collect.Lists;
import com.lovetropics.lib.slideshow.SlideshowApi;
import com.lovetropics.minigames.common.core.game.GameResult;
import com.lovetropics.minigames.common.core.game.GameStopReason;
import com.lovetropics.minigames.common.core.game.IGameDefinition;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.PendingSubPhase;
import com.lovetropics.minigames.common.core.game.PlayerIsolation;
import com.lovetropics.minigames.common.core.game.SpawnBuilder;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.GameEventListeners;
import com.lovetropics.minigames.common.core.game.behavior.event.GameEventType;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.SubGameEvents;
import com.lovetropics.minigames.common.core.game.command.GameCommandSet;
import com.lovetropics.minigames.common.core.game.map.GameMap;
import com.lovetropics.minigames.common.core.game.player.MutablePlayerSet;
import com.lovetropics.minigames.common.core.game.player.PlayerIterable;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.state.GameStateMap;
import com.lovetropics.minigames.common.core.game.state.statistics.PlayerKey;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.util.GameScheduler;
import com.lovetropics.minigames.common.core.game.util.GameTexts;
import com.lovetropics.minigames.common.core.game.util.TeamAllocator;
import com.lovetropics.minigames.common.core.map.MapRegions;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/// Think of a GamePhase like an act in a play, where the play is a GameInstance
public class GamePhase implements IGamePhase {
	private static final Logger LOGGER = LogUtils.getLogger();

	/* package-private */ final GameInstance game;
	// TODO: Narrow the data that we need to expose from this
	private final IGameDefinition definition;
	private final @Nullable GamePhase parentPhase;

	private final ServerLevel level;
	private final GameMap map;
	private final GameStateMap phaseState = new GameStateMap();

	private final MutablePlayerSet allPlayers;
	private final Map<UUID, PlayerRole> roles = new HashMap<>();
	private final Map<PlayerRole, PlayerSet> playersByRole = new EnumMap<>(PlayerRole.class);

	private final GameEventListeners events = new GameEventListeners();

	private long ticks;
	private boolean started;
	private final boolean focusedLive;

	private final GameScheduler scheduler = new GameScheduler();
	private GameCommandSet commandSet = GameCommandSet.EMPTY;

	private final List<GamePhase> subPhases = new ArrayList<>();
	private final List<PendingSubPhaseImpl> pendingSubPhases = new ArrayList<>();

	private boolean handlingJoin;

	private @Nullable GameStopReason stopReason;
	private boolean destroyed;

	/* package-private */ GamePhase(GameInstance game, @Nullable GamePhase parentPhase, GameMap map, IGameDefinition definition, IGameBehavior behavior) {
		this.game = game;
		this.parentPhase = parentPhase;
		this.definition = definition;

		// TODO: Don't do that :(
		focusedLive = game.lobby().metadata.visibility().isFocusedLive();

		MinecraftServer server = game.server();
		level = Objects.requireNonNull(server.getLevel(map.dimension()), "Game dimension not loaded");
		this.map = map;

		allPlayers = new MutablePlayerSet(server);
		for (PlayerRole role : PlayerRole.ROLES) {
			playersByRole.put(role, allPlayers.filter(player -> roles.get(player.getUUID()) == role));
		}

		String mapName = map.name();
		if (mapName != null) {
			statistics().global().set(StatisticKey.MAP, mapName);
		}

		behavior.registerState(this, phaseState, instanceState());
		behavior.register(this, events);
		invoker(GamePhaseEvents.CREATE).create();

		Identifier introSlideshow = definition().introSlideshow();
		if (introSlideshow != null) {
			events.listen(GamePlayerEvents.JOIN, player ->
					SlideshowApi.preload(player, introSlideshow)
			);
		}
	}

	public void assignRolesFrom(TeamAllocator<PlayerRole, PlayerKey> roleAllocator) {
		roleAllocator.setSizeForTeam(PlayerRole.PARTICIPANT, definition().getMaximumParticipantCount());
		invoker(GamePlayerEvents.ALLOCATE_ROLES).onAllocateRoles(roleAllocator);

		roleAllocator.allocate(this::setPlayerRole);
	}

	public GameResult<Unit> addPlayersAndStart(PlayerIterable players, @Nullable PlayerKey initiator) {
		if (started) {
			return GameResult.error(GameTexts.Commands.GAME_ALREADY_STARTED);
		}

		started = true;

		commandSet = GameCommandSet.registerFor(this);

		try {
			invoker(GamePlayerEvents.BEFORE_ADD_PLAYERS).beforeAddPlayers(
					collectPlayersWithRole(players, PlayerRole.PARTICIPANT),
					collectPlayersWithRole(players, PlayerRole.SPECTATOR)
			);
		} catch (Exception e) {
			LOGGER.error("Failed to prepare players for join", e);
		}

		for (ServerPlayer player : players.shuffledCopy(random())) {
			addPlayerDirectly(player, false);
		}

		try {
			invoker(GamePhaseEvents.START).start(initiator);
		} catch (Exception e) {
			return GameResult.fromException("Failed to start game", e);
		}

		return GameResult.ok();
	}

	private Set<PlayerKey> collectPlayersWithRole(PlayerIterable players, PlayerRole role) {
		return players.stream()
				.filter(player -> getRoleFor(player) == role)
				.map(PlayerKey::from)
				.collect(Collectors.toSet());
	}

	private ServerPlayer addPlayerDirectly(ServerPlayer player, boolean explicitlyJoined) {
		PlayerRole role = getRoleFor(player);

		ServerPlayer newPlayer;
		Consumer<ServerPlayer> initializer;

		CompoundTag playerTag = invoker(GamePlayerEvents.LOAD).tryLoad(PlayerKey.from(player), role);
		if (playerTag != null) {
			newPlayer = PlayerIsolation.INSTANCE.reloadPlayerFromTag(playerTag, player);
			initializer = p -> {
			};
		} else {
			SpawnBuilder spawn = determinePlayerSpawn(player, role);
			newPlayer = PlayerIsolation.INSTANCE.teleportTo(player, spawn.level(), spawn.position(), spawn.yRot(), spawn.xRot());
			initializer = spawn::applyInitializers;
		}

		allPlayers.add(newPlayer);

		try {
			handlingJoin = explicitlyJoined;
			invoker(GamePlayerEvents.ADD).onAdd(newPlayer);
			initializer.accept(newPlayer);

			invoker(GamePlayerEvents.SET_ROLE).onSetRole(newPlayer, role, null);

			if (explicitlyJoined) {
				invoker(GamePlayerEvents.JOIN).onAdd(newPlayer);
			}
		} catch (Exception e) {
			LOGGER.error("Failed to dispatch player add event", e);
		} finally {
			handlingJoin = false;
		}

		return newPlayer;
	}

	private SpawnBuilder determinePlayerSpawn(ServerPlayer player, @Nullable PlayerRole role) {
		SpawnBuilder spawn = new SpawnBuilder(player);
		// Just have some kind of default, even if unreasonable
		spawn.teleportTo(level(), new Vec3(0.0, 64.0, 0.0), 0.0f, 0.0f);
		try {
			invoker(GamePlayerEvents.SPAWN).onSpawn(player.getUUID(), spawn, role);
		} catch (Exception e) {
			LOGGER.error("Failed to dispatch player spawn event", e);
		}
		return spawn;
	}

	public @Nullable GameStopReason stopReason() {
		return stopReason;
	}

	public boolean isStopped() {
		return stopReason != null;
	}

	public boolean tick() {
		if (parentPhase == null) {
			tickTopLevel();
		}

		if (stopReason != null) {
			if (isReadyToDestroy()) {
				destroy();
				return true;
			}
			return false;
		}

		if (!started) {
			return false;
		}

		if (!pendingSubPhases.isEmpty()) {
			List<PendingSubPhaseImpl> readySubPhases = new ArrayList<>();
			pendingSubPhases.removeIf(pending -> {
				if (pending.future.isDone()) {
					readySubPhases.add(pending);
					return true;
				}
				return false;
			});
			// Setting a sub-phase might have side effects (i.e. queuing a new sub-phase), run in a separate pass
			readySubPhases.forEach(this::registerSubPhase);
		}

		try {
			scheduler.tick();
			invoker(GamePhaseEvents.TICK).tick();
		} catch (Exception e) {
			cancelWithError(e);
		}
		ticks++;

		return false;
	}

	private void tickTopLevel() {
		handleStoppedSubPhases();
	}

	// Note: not restoring players to the main world once the top phase is closed, as the lobby will transfer horizontally when ready
	private void handleStoppedSubPhases() {
		subPhases.removeIf(subPhase -> {
			subPhase.handleStoppedSubPhases();
			if (subPhase.stopReason == null) {
				return false;
			}
			// We don't want to pull players up that are going to be transferred horizontally anyway once the next phase loads
			// For simplicity now, just wait until all sub-phases are ready
			if (!pendingSubPhases.isEmpty()) {
				return false;
			}
			for (ServerPlayer player : subPhase.removeAllPlayers()) {
				addPlayerDirectly(player, false);
			}
			return true;
		});
	}

	private void registerSubPhase(PendingSubPhaseImpl pending) {
		List<ServerPlayer> playersToAdd = new ArrayList<>();
		for (ServerPlayer player : pending.queuedPlayers) {
			if (allPlayers.contains(player)) {
				removePlayerDirectly(player, false);
				playersToAdd.add(player);
			} else {
				// Transfer horizontally if possible so that we don't need to pull players up into the top phase
				for (GamePhase subPhase : subPhases) {
					if (subPhase.allPlayers().contains(player)) {
						subPhase.removePlayerDirectly(player, false);
						playersToAdd.add(player);
						break;
					}
				}
			}
		}

		try {
			GamePhase phase = pending.future.join();
			pending.registered = true;
			subPhases.add(phase);

			for (PendingSubPhase.CreateHandler handler : pending.createHandlers) {
				handler.onCreate(phase, phase.events);
			}
			invoker(SubGameEvents.CREATE).onCreateSubGame(phase, phase.events);

			phase.roles.putAll(roles);
			phase.addPlayersAndStart(PlayerIterable.from(playersToAdd), null);
		} catch (Exception e) {
			LOGGER.error("Failed to create sub-phase", e);
			for (ServerPlayer player : playersToAdd) {
				addPlayerDirectly(player, false);
			}
			for (Consumer<Exception> errorHandler : pending.errorHandlers) {
				errorHandler.accept(e);
			}
		}
	}

	@Override
	public void returnToParent(ServerPlayer player) {
		if (parentPhase == null) {
			removePlayerDirectly(player, true);
			PlayerIsolation.INSTANCE.restore(player);
		} else {
			removePlayerDirectly(player, false);
			parentPhase.addPlayerDirectly(player, false);
		}
	}

	@Override
	public void transferPlayerTo(ServerPlayer player, IGamePhase subPhase) {
		if (!subPhases.contains(subPhase)) {
			throw new IllegalArgumentException("Cannot transfer player to phase that is not a direct child of this phase");
		}
		removePlayerDirectly(player, false);
		GamePhase subGamePhase = (GamePhase) subPhase;
		if (handlingJoin) {
			PlayerRole role = subGamePhase.selectRoleForJoin(player, getRoleFor(player));
			subPhase.setPlayerRole(player, role);
		}
		subGamePhase.addPlayerDirectly(player, handlingJoin);
	}

	@Override
	public PendingSubPhase createSubPhase(IGameDefinition subGameConfig) {
		if (isStopped()) {
			throw new IllegalStateException("Cannot create sub-phase for stopped game");
		}
		CompletableFuture<GamePhase> future = GamePhaseManager.get().createSubPhase(this, subGameConfig);
		PendingSubPhaseImpl pendingPhase = new PendingSubPhaseImpl(future, server());
		pendingSubPhases.add(pendingPhase);
		return pendingPhase;
	}

	@Override
	public GameStateMap state() {
		return phaseState;
	}

	@Override
	public GameStateMap instanceState() {
		return game.instanceState();
	}

	@Override
	public PlayerSet allPlayers(boolean includeSubPhases) {
		if (includeSubPhases) {
			return PlayerSet.wrap(server(), roles.keySet());
		}
		return allPlayers;
	}

	@Override
	public IGameDefinition definition() {
		return definition;
	}

	@Override
	public <T> T invoker(GameEventType<T> type) {
		return events.invoker(type);
	}

	@Override
	public boolean setPlayerRole(ServerPlayer player, @Nullable PlayerRole role) {
		return setPlayerRole(PlayerKey.from(player), role);
	}

	private boolean setPlayerRole(PlayerKey player, @Nullable PlayerRole role) {
		PlayerRole lastRole;
		if (role != null) {
			lastRole = roles.put(player.id(), role);
		} else {
			lastRole = roles.remove(player.id());
		}

		if (role == lastRole) {
			return false;
		}

		// If we haven't added this player yet, just track state for now
		ServerPlayer playerEntity = allPlayers.getPlayerBy(player);
		if (playerEntity != null) {
			applyRoleChange(playerEntity, role, lastRole);
		}

		return true;
	}

	private void applyRoleChange(ServerPlayer player, @Nullable PlayerRole role, @Nullable PlayerRole lastRole) {
		try {
			SpawnBuilder spawn = determinePlayerSpawn(player, role);
			spawn.teleportAndApply(player);
			invoker(GamePlayerEvents.SET_ROLE).onSetRole(player, role, lastRole);
		} catch (Exception e) {
			LOGGER.error("Failed to dispatch player set role event", e);
		}
	}

	public ServerPlayer addPlayer(ServerPlayer player, PlayerRole requestedRole) {
		PlayerRole role = selectRoleForJoin(player, requestedRole);
		setPlayerRole(player, role);
		return addPlayerDirectly(player, true);
	}

	private @Nullable PlayerRole selectRoleForJoin(ServerPlayer player, @Nullable PlayerRole requestedRole) {
		try {
			// The player hasn't joined the game yet, so don't expose the player instance
			return invoker(GamePlayerEvents.SELECT_ROLE_ON_JOIN).selectRole(PlayerKey.from(player), requestedRole);
		} catch (Exception e) {
			LOGGER.error("Failed to select role for {}, joining as spectator", player.getScoreboardName(), e);
			return PlayerRole.SPECTATOR;
		}
	}

	public ServerPlayer removePlayer(ServerPlayer player, boolean loggingOut) {
		// To ensure that the top-level game gets notified of leaves properly, we need to pull the player out step-by-step
		for (GamePhase subPhase : subPhases) {
			if (subPhase.allPlayers.contains(player)) {
				player = subPhase.removePlayer(player, loggingOut);
				break;
			}
		}
		removePlayerDirectly(player, true);
		if (parentPhase != null) {
			return parentPhase.addPlayerDirectly(player, false);
		} else {
			if (loggingOut) {
				// Don't try to restore the player if they're logging out, as we never save their in-game state anyway
				return player;
			}
			return PlayerIsolation.INSTANCE.restore(player);
		}
	}

	private void removePlayerDirectly(ServerPlayer player, boolean explicitlyLeft) {
		if (!allPlayers.remove(player)) {
			throw new IllegalArgumentException(player.getScoreboardName() + " is not in this phase, cannot be removed");
		}

		if (explicitlyLeft) {
			try {
				invoker(GamePlayerEvents.LEAVE).onRemove(player);
			} catch (Exception e) {
				LOGGER.error("Failed to dispatch player leave event", e);
			}
		}

		try {
			invoker(GamePlayerEvents.REMOVE).onRemove(player);
		} catch (Exception e) {
			LOGGER.error("Failed to dispatch player leave event", e);
		}

		if (explicitlyLeft) {
			roles.remove(player.getUUID());
		}
	}

	public List<ServerPlayer> removeAllPlayers() {
		for (GamePhase subPhase : subPhases) {
			for (ServerPlayer player : subPhase.allPlayers.shuffledCopy(random())) {
				subPhase.removePlayerDirectly(player, false);
				addPlayerDirectly(player, false);
			}
		}

		List<ServerPlayer> players = Lists.newArrayList(allPlayers);
		allPlayers.clear();

		try {
			for (ServerPlayer player : players) {
				invoker(GamePlayerEvents.REMOVE).onRemove(player);
			}
		} catch (Exception e) {
			LOGGER.error("Unknown error while removing players", e);
		}

		return players;
	}

	// TODO: This doesn't respect sub-game hierarchy at all, and is extremely naive when it comes to passing through roles. Please re-evaluate.
	/* package-private */ ServerPlayer teleportFrom(ServerPlayer player, @Nullable GamePhase fromPhase) {
		if (fromPhase != null) {
			PlayerRole oldRole = fromPhase.getRoleFor(player);
			fromPhase.removePlayerDirectly(player, false);
			roles.putIfAbsent(player.getUUID(), oldRole);
		}
		return addPlayerDirectly(player, false);
	}

	public void cancelWithError(Exception exception) {
		LOGGER.error("Game canceled due to exception", exception);
		requestStop(GameStopReason.errored(Component.literal("Game stopped due to exception: " + exception)));
	}

	@Override
	public GameResult<Unit> requestStop(GameStopReason reason) {
		if (stopReason != null) {
			return GameResult.error(GameTexts.Commands.GAME_ALREADY_STOPPED);
		}

		for (GamePhase subPhase : subPhases) {
			subPhase.requestStop(reason);
		}

		stopReason = reason;

		try {
			invoker(GamePhaseEvents.STOP).stop(reason);

			if (reason.isFinished()) {
				invoker(GamePhaseEvents.FINISH).finish();
			}

			return GameResult.ok();
		} catch (Exception e) {
			return GameResult.fromException("Unknown error while stopping game", e);
		}
	}

	private void destroy() {
		if (destroyed) {
			return;
		}
		destroyed = true;

		for (PendingSubPhaseImpl pendingSubPhase : pendingSubPhases) {
			GameStopReason stopReason = Objects.requireNonNullElse(this.stopReason, GameStopReason.canceled());
			pendingSubPhase.future.thenAcceptAsync(phase -> phase.requestStop(stopReason), server());
		}
		pendingSubPhases.clear();

		subPhases.forEach(GamePhase::destroy);
		subPhases.clear();

		try {
			invoker(GamePhaseEvents.DESTROY).destroy();
		} catch (Exception e) {
			LOGGER.error("Unknown error while stopping game", e);
		} finally {
			map.close(this);
		}
	}

	private boolean isReadyToDestroy() {
		// The phase itself isn't responsible for moving players out - so wait for them to leave before we clean up
		if (stopReason == null || !allPlayers.isEmpty()) {
			return false;
		}
		for (GamePhase subPhase : subPhases) {
			if (!subPhase.isReadyToDestroy()) {
				return false;
			}
		}
		return true;
	}

	public void stopForServerShutdown() {
		for (GamePhase subPhase : subPhases) {
			subPhase.stopForServerShutdown();
		}
		requestStop(GameStopReason.serverStopping());
		removeAllPlayers();
		destroy();
	}

	@Override
	public PlayerSet getPlayersWithRole(PlayerRole role) {
		return playersByRole.get(role);
	}

	@Override
	public @Nullable PlayerRole getRoleFor(ServerPlayer player) {
		return roles.get(player.getUUID());
	}

	@Override
	public MapRegions mapRegions() {
		return map.mapRegions();
	}

	@Override
	public ResourceKey<Level> dimension() {
		return map.dimension();
	}

	@Override
	public List<ResourceKey<Level>> dimensions() {
		return map.allDimensions();
	}

	@Override
	public ServerLevel level() {
		return level;
	}

	@Override
	public GameScheduler scheduler() {
		return scheduler;
	}

	@Override
	public long ticks() {
		return ticks;
	}

	@Override
	public boolean isFocusedLive() {
		return focusedLive;
	}

	public GameCommandSet getCommandSet() {
		return commandSet;
	}

	public Stream<GamePhase> allSubPhases() {
		return Stream.concat(Stream.of(this), subPhases.stream().flatMap(GamePhase::allSubPhases));
	}

	private class PendingSubPhaseImpl implements PendingSubPhase {
		private final CompletableFuture<GamePhase> future;
		private final MutablePlayerSet queuedPlayers;
		private final List<CreateHandler> createHandlers = new ArrayList<>();
		private final List<Consumer<Exception>> errorHandlers = new ArrayList<>();
		private boolean registered;

		private PendingSubPhaseImpl(CompletableFuture<GamePhase> future, MinecraftServer server) {
			this.future = future;
			queuedPlayers = new MutablePlayerSet(server);
		}

		private void checkPending() {
			if (registered) {
				throw new IllegalArgumentException("Sub-phase has already been registered");
			}
		}

		@Override
		public void queuePlayer(ServerPlayer player) {
			if (registered) {
				transferPlayerTo(player, future.join());
			} else {
				queuedPlayers.add(player);
			}
		}

		@Override
		public void whenCreated(CreateHandler handler) {
			checkPending();
			createHandlers.add(handler);
		}

		@Override
		public void whenErrored(Consumer<Exception> consumer) {
			checkPending();
			errorHandlers.add(consumer);
		}
	}
}
