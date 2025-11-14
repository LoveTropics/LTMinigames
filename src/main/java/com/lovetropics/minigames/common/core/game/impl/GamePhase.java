package com.lovetropics.minigames.common.core.game.impl;

import com.google.common.collect.Lists;
import com.lovetropics.lib.slideshow.SlideshowApi;
import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.river_race.event.RiverRaceEvents;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.GamePhaseType;
import com.lovetropics.minigames.common.core.game.GameResult;
import com.lovetropics.minigames.common.core.game.GameStopReason;
import com.lovetropics.minigames.common.core.game.IGameDefinition;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.IGamePhaseDefinition;
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
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
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

	final GameInstance game;
	final MinecraftServer server;
	final IGameDefinition gameDefinition;
	final IGamePhaseDefinition phaseDefinition;
	final GamePhaseType phaseType;

	final GameMap map;
	final BehaviorList behaviors;
	final GameStateMap phaseState = new GameStateMap();

	final EnumMap<PlayerRole, MutablePlayerSet> roles = new EnumMap<>(PlayerRole.class);
	protected final Set<UUID> addedPlayers = new ObjectArraySet<>();
	private boolean assignedRoles;

	final GameEventListeners events = new GameEventListeners();

	long startTime;
	@Nullable
	GameStopReason stopped;
	boolean destroyed;

	private ControlCommandInvoker controlCommands = ControlCommandInvoker.EMPTY;

	private final GameScheduler scheduler = new GameScheduler();

	private final Queue<GameConfig> subPhaseQueue = new ArrayDeque<>();
	@Nullable
	private GamePhase subPhase;

	// TODO: Big hack - can we do something better by splitting what we expose to game impls vs what we expose to the outside?
	//       Some behaviors such as spectator_chase check the spectator list when the player is removed - but that spectator list didn't get the player removed
	private boolean hideRoles;

	protected GamePhase(GameInstance game, IGameDefinition gameDefinition, IGamePhaseDefinition phaseDefinition, GamePhaseType phaseType, GameMap map, BehaviorList behaviors) {
		this.game = game;
		server = game.server();
		this.gameDefinition = gameDefinition;
		this.phaseDefinition = phaseDefinition;
		this.phaseType = phaseType;

		this.map = map;
		this.behaviors = behaviors;

		for (PlayerRole role : PlayerRole.ROLES) {
			roles.put(role, new MutablePlayerSet(server));
		}
	}

	public static CompletableFuture<GameResult<GamePhase>> create(GameInstance game, IGameDefinition gameDefinition, IGamePhaseDefinition phaseDefinition, GamePhaseType phaseType) {
		MinecraftServer server = game.server();

		GameResult<Unit> result = game.lobby.manager.canStartGamePhase(phaseDefinition);
		if (result.isError()) {
			return CompletableFuture.completedFuture(result.castError());
		}

		BehaviorList behaviors = phaseDefinition.createBehaviors();

		CompletableFuture<GameResult<GamePhase>> future = phaseDefinition.getMap().open(server)
				.thenApply(r -> r.map(map -> new GamePhase(game, gameDefinition, phaseDefinition, phaseType, map, behaviors)));

		return GameResult.handleException("Unknown exception starting game phase", future);
	}

	GameResult<Unit> start() {
		try {
			behaviors.registerTo(this, events);
		} catch (GameException e) {
			return GameResult.error(e.getTextMessage());
		} catch (Exception e) {
			return GameResult.error(Component.literal(e.getClass() + ": " + e.getMessage()));
		}

		final String mapName = map.name();
		if (mapName != null) {
			statistics().global().set(StatisticKey.MAP, mapName);
		}

		startTime = level().getGameTime();

		try {
			allocateRoles();

			invoker(GamePhaseEvents.CREATE).create(participants().stream().map(PlayerKey::from).collect(Collectors.toSet()));

			List<ServerPlayer> shuffledPlayers = Lists.newArrayList(allPlayers());
			Collections.shuffle(shuffledPlayers);

			for (ServerPlayer player : shuffledPlayers) {
				addAndSpawnPlayer(player, getRoleFor(player));
			}

			invoker(GamePhaseEvents.START).start(game.lobby.getMetadata().initiator());
		} catch (Exception e) {
			return GameResult.fromException("Failed to start game", e);
		}

		controlCommands = buildControlCommandInvoker();

		return GameResult.ok();
	}

	private ControlCommandInvoker buildControlCommandInvoker() {
		ControlCommands commands = new ControlCommands();
		invoker(GamePhaseEvents.REGISTER_COMMANDS).register(commands);
		return commands;
	}

	private ServerPlayer addAndSpawnPlayer(ServerPlayer player, @Nullable PlayerRole role) {
		ResourceLocation introSlideshow = definition().introSlideshow();
		if (phaseType == GamePhaseType.WAITING && introSlideshow != null) {
			SlideshowApi.preload(player, introSlideshow);
		}

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
				LoveTropics.LOGGER.error("Failed to dispatch player spawn event", e);
			}
			newPlayer = PlayerIsolation.INSTANCE.teleportTo(player, spawn.level(), spawn.position(), spawn.yRot(), spawn.xRot());
			initializer = spawn::applyInitializers;
		}

		invoker(GamePlayerEvents.ADD).onAdd(newPlayer);
		initializer.accept(newPlayer);

		invoker(GamePlayerEvents.SET_ROLE).onSetRole(newPlayer, role, null);

		addedPlayers.add(player.getUUID());

		return newPlayer;
	}

	@Nullable
	GameStopReason tick() {
		if (subPhase != null) {
			if (subPhase.tick() != null) {
				GamePhase lastPhase = subPhase;
				startNextQueuedMicrogame().whenComplete((newGame, throwable) -> {
					if (throwable != null || !newGame) {
						returnHere(lastPhase);
					}
					if (throwable != null) {
						LOGGER.error("Failed to start next queued micro-game", throwable);
					}
				});
				return null;
			}
		} else {
			try {
				scheduler.tick();
				invoker(GamePhaseEvents.TICK).tick();
			} catch (Exception e) {
				cancelWithError(e);
			}
		}

		return stopped;
	}

	@Override
	public GameStateMap state() {
		return phaseState;
	}

	@Override
	public GameStateMap instanceState() {
		return game.instanceState();
	}

	public GamePhaseType phaseType() {
		return phaseType;
	}

	@Override
	public PlayerSet allPlayers() {
		return game.allPlayers();
	}

	@Override
	public IGameDefinition definition() {
		return gameDefinition;
	}

	public IGamePhaseDefinition phaseDefinition() {
		return phaseDefinition;
	}

	@Override
	public <T> T invoker(GameEventType<T> type) {
		return events.invoker(type);
	}

	private void allocateRoles() {
		// TODO: somehow if a player is in a lobby and then leaves they can get in such a state as to join late and be joined as a participant when clicking 'play'
		if (assignedRoles) {
			return;
		}

		LOGGER.debug("Allocating players to roles based on selections: {}", game.lobby.getPlayers().getRoleSelections());
		TeamAllocator<PlayerRole, PlayerKey> allocator = game.lobby.getPlayers().createRoleAllocator();
		allocator.setSizeForTeam(PlayerRole.PARTICIPANT, definition().getMaximumParticipantCount());
		invoker(GamePlayerEvents.ALLOCATE_ROLES).onAllocateRoles(allocator);

		allocator.allocate((playerKey, role) -> {
			ServerPlayer player = allPlayers().getPlayerBy(playerKey);
			if (player != null) {
				setPlayerRole(player, role);
			}
		});

		assignedRoles = true;
	}

	public void assignRolesFrom(IGamePhase topLevelGame) {
		for (PlayerRole role : PlayerRole.values()) {
			for (ServerPlayer player : topLevelGame.getPlayersWithRole(role)) {
				setPlayerRole(player, role);
			}
		}
		assignedRoles = true;
	}

	@Override
	public boolean setPlayerRole(ServerPlayer player, @Nullable PlayerRole role) {
		PlayerRole lastRole = getRoleFor(player);
		if (role == lastRole) {
			return false;
		}

		if (lastRole != null) {
			roles.get(lastRole).remove(player);
		}
		if (role != null) {
			roles.get(role).add(player);
		}

		// If we haven't added this player yet, just track state for now
		if (addedPlayers.contains(player.getUUID())) {
			onSetPlayerRole(player, role, lastRole);
		}

		return true;
	}

	private void onSetPlayerRole(ServerPlayer player, @Nullable PlayerRole role, @Nullable PlayerRole lastRole) {
		try {
			SpawnBuilder spawn = new SpawnBuilder(player);
			invoker(GamePlayerEvents.SPAWN).onSpawn(player.getUUID(), spawn, role);
			spawn.teleportAndApply(player);
			if (role != lastRole) {
				invoker(GamePlayerEvents.SET_ROLE).onSetRole(player, role, lastRole);
			}
		} catch (Exception e) {
			LoveTropics.LOGGER.warn("Failed to dispatch player set role event", e);
		}
	}

	ServerPlayer onPlayerJoin(ServerPlayer player) {
		try {
			// Bit of a hack - might already have been assigned a role from the top-level game
			PlayerRole role = getRoleFor(player);
			if (role == null) {
				// The player hasn't joined the game yet, so don't expose the player instance
				PlayerRole selectedRole = game.lobby.getPlayers().getRoleSelections().getSelectedRoleFor(player.getUUID());
				role = invoker(GamePlayerEvents.SELECT_ROLE_ON_JOIN).selectRole(PlayerKey.from(player), selectedRole);
				setPlayerRole(player, role);
			}
			ServerPlayer newPlayer = addAndSpawnPlayer(player, role);
			invoker(GamePlayerEvents.JOIN).onAdd(newPlayer);

			if (subPhase != null) {
				// Let the top-level game decide how the player can join, and then just pass them along
				subPhase.assignRolesFrom(this);
				movePlayerToSubPhase(player);
				return subPhase.onPlayerJoin(newPlayer);
			}

			return newPlayer;
		} catch (Exception e) {
			LoveTropics.LOGGER.warn("Failed to dispatch player join event", e);
			return player;
		}
	}

	ServerPlayer onPlayerLeave(ServerPlayer player, boolean loggingOut) {
		if (subPhase != null) {
			// To ensure that the top-level game gets notified properly, we need to pull the player out step-by-step
			try {
				subPhase.invoker(GamePlayerEvents.LEAVE).onRemove(player);
			} catch (Exception e) {
				LoveTropics.LOGGER.warn("Failed to dispatch player leave event", e);
			}
			player = returnPlayerToParentPhase(subPhase, player);
		}

		try {
			invoker(GamePlayerEvents.LEAVE).onRemove(player);
		} catch (Exception e) {
			LoveTropics.LOGGER.warn("Failed to dispatch player leave event", e);
		}

		for (PlayerRole role : PlayerRole.ROLES) {
			roles.get(role).remove(player);
		}

		try {
			addedPlayers.remove(player.getUUID());
			invoker(GamePlayerEvents.REMOVE).onRemove(player);
		} catch (Exception e) {
			LoveTropics.LOGGER.warn("Failed to dispatch player leave event", e);
		}

		// Don't try to restore the player if they're logging out, as we never save their in-game state anyway
		if (loggingOut) {
			return player;
		}
		return PlayerIsolation.INSTANCE.restore(player);
	}

	void removePlayer(ServerPlayer player) {
		addedPlayers.remove(player.getUUID());
		for (PlayerRole role : PlayerRole.ROLES) {
			roles.get(role).remove(player);
		}
		try {
			invoker(GamePlayerEvents.REMOVE).onRemove(player);
		} catch (Exception e) {
			LoveTropics.LOGGER.warn("Failed to dispatch player leave event", e);
		}
	}

	public void cancelWithError(Exception exception) {
		LoveTropics.LOGGER.warn("Game canceled due to exception", exception);
		requestStop(GameStopReason.errored(Component.literal("Game stopped due to exception: " + exception)));
	}

	@Override
	public GameResult<Unit> requestStop(GameStopReason reason) {
		if (stopped != null) {
			return GameResult.error(GameTexts.Commands.GAME_ALREADY_STOPPED);
		}

		stopped = reason;

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

	void destroy() {
		if (destroyed) {
			return;
		}
		destroyed = true;

		destroySubGame();
		requestStop(GameStopReason.canceled());

		try {
			for (ServerPlayer player : allPlayers()) {
				addedPlayers.remove(player.getUUID());
				invoker(GamePlayerEvents.REMOVE).onRemove(player);
			}

			invoker(GamePhaseEvents.DESTROY).destroy();
		} catch (Exception e) {
			LoveTropics.LOGGER.warn("Unknown error while stopping game", e);
		} finally {
			map.close(this);
		}
	}

	@Override
	public PlayerSet getPlayersWithRole(PlayerRole role) {
		if (hideRoles) {
			return PlayerSet.EMPTY;
		}
		return roles.get(role);
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
		return server.getLevel(map.dimension());
	}

	@Override
	public GameScheduler scheduler() {
		return scheduler;
	}

	@Override
	public long ticks() {
		if (startTime == 0) {
			return 0;
		}
		return level().getGameTime() - startTime;
	}

	@Override
	public IGamePhase getTopPhase() {
		return Objects.requireNonNullElse(game.lobby.getTopPhase(), this);
	}

	@Override
	public boolean isFocusedLive() {
		return game.lobby.metadata.visibility().isFocusedLive();
	}

	public GamePhase getActivePhase() {
		return Objects.requireNonNullElse(subPhase, this);
	}

	public void startSubPhase(GamePhase subPhase) {
		this.subPhase = subPhase;
		GameManager.INSTANCE.addGamePhaseToDimension(subPhase.dimension(), subPhase);
		subPhase.assignRolesFrom(this);
		hideRoles = true;
		for (ServerPlayer player : allPlayers()) {
			movePlayerToSubPhase(player);
		}
		hideRoles = false;

		subPhase.events.listen(GamePhaseEvents.CREATE, participants ->
				invoker(SubGameEvents.CREATE).onCreateSubGame(subPhase, subPhase.events)
		);
		subPhase.start();
	}

	private void movePlayerToSubPhase(ServerPlayer player) {
		if (addedPlayers.remove(player.getUUID())) {
			invoker(GamePlayerEvents.REMOVE).onRemove(player);
		}
	}

	private void returnHere(GamePhase fromSubPhase) {
		List<ServerPlayer> shuffledPlayers = Lists.newArrayList(allPlayers());
		Collections.shuffle(shuffledPlayers);
		for (ServerPlayer player : shuffledPlayers) {
			returnPlayerToParentPhase(fromSubPhase, player);
		}
		invoker(SubGameEvents.RETURN_TO_TOP).onReturnToTopGame();
	}

	private ServerPlayer returnPlayerToParentPhase(GamePhase fromSubPhase, ServerPlayer player) {
		fromSubPhase.removePlayer(player);
		return addAndSpawnPlayer(player, getRoleFor(player));
	}

	private void destroySubGame() {
		if (subPhase != null) {
			subPhase.destroy();
			GameManager.INSTANCE.removeGamePhaseFromDimension(subPhase.dimension(), subPhase);
			subPhase = null;
		}
	}

	public void clearQueuedGames() {
		subPhaseQueue.clear();
	}

	public void queueGames(List<GameConfig> games) {
		subPhaseQueue.addAll(games);
	}

	public CompletableFuture<Boolean> startNextQueuedMicrogame() {
		destroySubGame();
		// No queued games left
		if (subPhaseQueue.isEmpty()) {
			return CompletableFuture.completedFuture(false);
		}
		final GameConfig nextGame = subPhaseQueue.remove();
		return GamePhase.create(game, nextGame, nextGame.getPlayingPhase(), GamePhaseType.PLAYING).thenApply(result -> {
			if (result.isOk()) {
				startSubPhase(result.getOk());
				return true;
			}
			LOGGER.error("Failed to start micro-game {} - {}", nextGame.id().toString(), result.getError().getString());
			return false;
		});
	}

	public ControlCommandInvoker controlCommands() {
		return controlCommands;
	}
}
