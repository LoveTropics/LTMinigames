package com.lovetropics.minigames.common.core.game.impl;

import com.google.common.collect.Lists;
import com.lovetropics.lib.slideshow.SlideshowApi;
import com.lovetropics.minigames.common.core.game.GameResult;
import com.lovetropics.minigames.common.core.game.GameStopReason;
import com.lovetropics.minigames.common.core.game.IGameDefinition;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.PlayerIsolation;
import com.lovetropics.minigames.common.core.game.SpawnBuilder;
import com.lovetropics.minigames.common.core.game.behavior.BehaviorList;
import com.lovetropics.minigames.common.core.game.behavior.event.GameEventListeners;
import com.lovetropics.minigames.common.core.game.behavior.event.GameEventType;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.SubGameEvents;
import com.lovetropics.minigames.common.core.game.config.GameConfig;
import com.lovetropics.minigames.common.core.game.map.GameMap;
import com.lovetropics.minigames.common.core.game.player.MutablePlayerSet;
import com.lovetropics.minigames.common.core.game.player.PlayerIterable;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.state.GameStateMap;
import com.lovetropics.minigames.common.core.game.state.control.ControlCommandInvoker;
import com.lovetropics.minigames.common.core.game.state.control.ControlCommands;
import com.lovetropics.minigames.common.core.game.state.statistics.PlayerKey;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.util.GameScheduler;
import com.lovetropics.minigames.common.core.game.util.GameTexts;
import com.lovetropics.minigames.common.core.game.util.TeamAllocator;
import com.lovetropics.minigames.common.core.map.MapRegions;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import javax.annotation.Nullable;
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

/**
 * Think of a GamePhase like an act in a play, where the play is a GameInstance
 */
public class GamePhase implements IGamePhase {
	private static final Logger LOGGER = LogUtils.getLogger();

	private static final long NOT_STARTED = -1;

	/* package-private */ final GameInstance game;
	private final IGameDefinition gameDefinition;

	private final ServerLevel level;
	private final GameMap map;
	private final GameStateMap phaseState = new GameStateMap();

	private final MutablePlayerSet allPlayers;
	private final Map<UUID, PlayerRole> roles = new HashMap<>();
	private final Map<PlayerRole, PlayerSet> playersByRole = new EnumMap<>(PlayerRole.class);

	private final GameEventListeners events = new GameEventListeners();

	private long startTime = NOT_STARTED;
	private boolean focusedLive;

	private final GameScheduler scheduler = new GameScheduler();
	private ControlCommandInvoker controlCommands = ControlCommandInvoker.EMPTY;

	@Nullable
	private GamePhase subPhase;
	@Nullable
	private CompletableFuture<GamePhase> queuedSubPhase;

	@Nullable
	private GameStopReason stopReason;
	private boolean destroyed;

	/* package-private */ GamePhase(GameInstance game, IGameDefinition gameDefinition, GameMap map, BehaviorList behaviors) {
		this.game = game;
		this.gameDefinition = gameDefinition;

		level = Objects.requireNonNull(game.server.getLevel(map.dimension()), "Game dimension not loaded");
		this.map = map;

		allPlayers = new MutablePlayerSet(game.server);
		for (PlayerRole role : PlayerRole.ROLES) {
			playersByRole.put(role, allPlayers.filter(player -> roles.get(player.getUUID()) == role));
		}

		String mapName = map.name();
		if (mapName != null) {
			statistics().global().set(StatisticKey.MAP, mapName);
		}

		behaviors.registerTo(this, events);
		invoker(GamePhaseEvents.CREATE).create();

		controlCommands = buildControlCommandInvoker();

		ResourceLocation introSlideshow = definition().introSlideshow();
		if (introSlideshow != null) {
			events.listen(GamePlayerEvents.JOIN, player ->
					SlideshowApi.preload(player, introSlideshow)
			);
		}
	}

	private ControlCommandInvoker buildControlCommandInvoker() {
		ControlCommands commands = new ControlCommands();
		invoker(GamePhaseEvents.REGISTER_COMMANDS).register(commands);
		return commands;
	}

	public void assignRolesFrom(TeamAllocator<PlayerRole, PlayerKey> roleAllocator) {
		roleAllocator.setSizeForTeam(PlayerRole.PARTICIPANT, definition().getMaximumParticipantCount());
		invoker(GamePlayerEvents.ALLOCATE_ROLES).onAllocateRoles(roleAllocator);

		roleAllocator.allocate(this::setPlayerRole);
	}

	public GameResult<Unit> addPlayersAndStart(PlayerIterable players, @Nullable PlayerKey initiator) {
		if (startTime != NOT_STARTED) {
			return GameResult.error(GameTexts.Commands.GAME_ALREADY_STARTED);
		}

		startTime = level.getGameTime();

		try {
			invoker(GamePlayerEvents.BEFORE_ADD_PLAYERS).beforeAddPlayers(
					collectPlayersWithRole(players, PlayerRole.PARTICIPANT),
					collectPlayersWithRole(players, PlayerRole.SPECTATOR)
			);
		} catch (Exception e) {
			LOGGER.error("Failed to prepare players for join", e);
		}

		for (ServerPlayer player : players.shuffledCopy(random())) {
			addAndSpawnPlayer(player);
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

	private ServerPlayer addAndSpawnPlayer(ServerPlayer player) {
		PlayerRole role = getRoleFor(player);

		ServerPlayer newPlayer;
		Consumer<ServerPlayer> initializer;

		CompoundTag playerTag = invoker(GamePlayerEvents.LOAD).tryLoad(PlayerKey.from(player), role);
		if (playerTag != null) {
			newPlayer = PlayerIsolation.INSTANCE.reloadPlayerFromTag(playerTag, player);
			initializer = p -> {};
		} else {
			SpawnBuilder spawn = new SpawnBuilder(player);
			try {
				invoker(GamePlayerEvents.SPAWN).onSpawn(player.getUUID(), spawn, role);
			} catch (Exception e) {
				LOGGER.error("Failed to dispatch player spawn event", e);
			}
			newPlayer = PlayerIsolation.INSTANCE.teleportTo(player, spawn.level(), spawn.position(), spawn.yRot(), spawn.xRot());
			initializer = spawn::applyInitializers;
		}

		allPlayers.add(newPlayer);

		try {
			invoker(GamePlayerEvents.ADD).onAdd(newPlayer);
			initializer.accept(newPlayer);

			invoker(GamePlayerEvents.SET_ROLE).onSetRole(newPlayer, role, null);
		} catch (Exception e) {
			LOGGER.error("Failed to dispatch player add event", e);
		}

		return newPlayer;
	}

	public void setFocusedLive(boolean focusedLive) {
		this.focusedLive = focusedLive;
	}

	@Nullable
	public GameStopReason stopReason() {
		return stopReason;
	}

	public boolean isStopped() {
		return stopReason != null;
	}

	public boolean tick() {
		if (stopReason != null) {
			if (isReadyToDestroy()) {
				destroy();
				return true;
			}
			return false;
		}

		tryStartQueuedSubPhase();

		if (subPhase != null) {
			tickSubPhase(subPhase);
		} else {
			tickTopLevel();
		}

		return false;
	}

	private void tickTopLevel() {
		if (startTime == NOT_STARTED) {
			return;
		}
		try {
			scheduler.tick();
			invoker(GamePhaseEvents.TICK).tick();
		} catch (Exception e) {
			cancelWithError(e);
		}
	}

	private void tickSubPhase(GamePhase subPhase) {
		if (!subPhase.isStopped()) {
			return;
		}
		if (queuedSubPhase != null) {
			// A new sub-phase is queued, don't transfer players back here just yet
			return;
		}
		this.subPhase = null;
		List<ServerPlayer> players = subPhase.removeAllPlayers();
		players.forEach(this::addAndSpawnPlayer);

		invoker(SubGameEvents.RETURN_TO_TOP).onReturnToTopGame();
	}

	private void tryStartQueuedSubPhase() {
		if (queuedSubPhase == null) {
			return;
		}
		try {
			GamePhase nextSubPhase = queuedSubPhase.getNow(null);
			if (nextSubPhase != null) {
				startSubPhase(nextSubPhase);
				queuedSubPhase = null;
			}
		} catch (Exception e) {
			LOGGER.error("Failed to start queued sub-phase", e);
			queuedSubPhase = null;
		}
	}

	private void startSubPhase(GamePhase subPhase) {
		List<ServerPlayer> allPlayers;
		if (this.subPhase != null) {
			// Transfer players horizontally if possible - the top game doesn't need to know about it!
			this.subPhase.requestStop(GameStopReason.canceled());
			allPlayers = this.subPhase.removeAllPlayers();
		} else {
			allPlayers = Lists.newArrayList(this.allPlayers);
			allPlayers.forEach(player -> removePlayer(player, false));
		}

		invoker(SubGameEvents.CREATE).onCreateSubGame(subPhase, subPhase.events);

		this.subPhase = subPhase;
		subPhase.roles.putAll(roles);
		subPhase.addPlayersAndStart(PlayerIterable.from(allPlayers), null);
	}

	@Override
	public void queueSubGame(GameConfig subGameConfig) {
		if (isStopped()) {
			return;
		}
		queuedSubPhase = GamePhaseManager.get().createPhase(game, game.server(), subGameConfig, subGameConfig.getPlayingPhase());
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
	public PlayerSet allPlayers() {
		return allPlayers;
	}

	@Override
	public IGameDefinition definition() {
		return gameDefinition;
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
			SpawnBuilder spawn = new SpawnBuilder(player);
			invoker(GamePlayerEvents.SPAWN).onSpawn(player.getUUID(), spawn, role);
			spawn.teleportAndApply(player);
			invoker(GamePlayerEvents.SET_ROLE).onSetRole(player, role, lastRole);
		} catch (Exception e) {
			LOGGER.error("Failed to dispatch player set role event", e);
		}
	}

	public ServerPlayer addPlayer(ServerPlayer player, PlayerRole requestedRole) {
		PlayerRole role;
		try {
			// The player hasn't joined the game yet, so don't expose the player instance
			role = invoker(GamePlayerEvents.SELECT_ROLE_ON_JOIN).selectRole(PlayerKey.from(player), requestedRole);
		} catch (Exception e) {
			LOGGER.error("Failed to select role for {}, joining as spectator", player.getScoreboardName(), e);
			role = PlayerRole.SPECTATOR;
		}

		return addPlayerWithRole(player, role);
	}

	private ServerPlayer addPlayerWithRole(ServerPlayer player, @Nullable PlayerRole role) {
		setPlayerRole(player, role);

		ServerPlayer newPlayer = addAndSpawnPlayer(player);
		try {
			invoker(GamePlayerEvents.JOIN).onAdd(newPlayer);
		} catch (Exception e) {
			LOGGER.error("Failed to dispatch player join event", e);
			return player;
		}

		if (subPhase != null) {
			// Let the top-level game decide how the player can join, and then just pass them along
			removePlayer(player, false);
			return subPhase.addPlayerWithRole(newPlayer, role);
		}

		return newPlayer;
	}

	public ServerPlayer removePlayer(ServerPlayer player, boolean explicitlyLeft) {
		if (subPhase != null && !allPlayers.contains(player)) {
			// To ensure that the top-level game gets notified properly, we need to pull the player out step-by-step
			player = subPhase.removePlayer(player, explicitlyLeft);
			player = addAndSpawnPlayer(player);
		}

		allPlayers.remove(player);

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

		return player;
	}

	public List<ServerPlayer> removeAllPlayers() {
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

	public void cancelWithError(Exception exception) {
		LOGGER.error("Game canceled due to exception", exception);
		requestStop(GameStopReason.errored(Component.literal("Game stopped due to exception: " + exception)));
	}

	@Override
	public GameResult<Unit> requestStop(GameStopReason reason) {
		if (stopReason != null) {
			return GameResult.error(GameTexts.Commands.GAME_ALREADY_STOPPED);
		}

		if (subPhase != null) {
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

		if (subPhase != null) {
			subPhase.destroy();
			subPhase = null;
		}

		try {
			invoker(GamePhaseEvents.DESTROY).destroy();
		} catch (Exception e) {
			LOGGER.error("Unknown error while stopping game", e);
		} finally {
			map.close(this);
		}
	}

	private boolean isReadyToDestroy() {
		return stopReason != null && allPlayers.isEmpty() && (subPhase == null || subPhase.isReadyToDestroy());
	}

	public void stopForServerShutdown() {
		if (subPhase != null) {
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
	public ServerLevel level() {
		return level;
	}

	@Override
	public GameScheduler scheduler() {
		return scheduler;
	}

	@Override
	public long ticks() {
		if (startTime == NOT_STARTED) {
			return 0;
		}
		return level.getGameTime() - startTime;
	}

	@Override
	public boolean isFocusedLive() {
		return focusedLive;
	}

	public GamePhase getActivePhase() {
		return Objects.requireNonNullElse(subPhase, this);
	}

	public ControlCommandInvoker controlCommands() {
		return controlCommands;
	}
}
