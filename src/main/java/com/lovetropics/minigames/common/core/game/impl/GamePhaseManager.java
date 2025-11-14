package com.lovetropics.minigames.common.core.game.impl;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGameDefinition;
import com.lovetropics.minigames.common.core.game.IGameLookup;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.IGamePhaseDefinition;
import com.lovetropics.minigames.common.core.game.behavior.BehaviorList;
import com.lovetropics.minigames.common.core.game.map.GameMap;
import com.lovetropics.minigames.common.core.game.map.IGameMapProvider;
import com.lovetropics.minigames.common.core.game.state.control.ControlCommandInvoker;
import com.lovetropics.minigames.common.core.game.util.GameTexts;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@EventBusSubscriber(modid = LoveTropics.ID)
public class GamePhaseManager implements IGameLookup {
	private static final GamePhaseManager INSTANCE = new GamePhaseManager();

	private final Queue<GamePhase> queuedGames = new ArrayDeque<>();

	private final Map<ResourceKey<Level>, List<GamePhase>> gamesByDimension = new Reference2ObjectOpenHashMap<>();

	public static GamePhaseManager get() {
		return INSTANCE;
	}

	public CompletableFuture<GamePhase> createPhase(GameInstance game, MinecraftServer server, IGameDefinition gameDefinition, IGamePhaseDefinition phaseDefinition) {
		try {
			checkCanAddGamePhase(phaseDefinition);
		} catch (GameException e) {
			return CompletableFuture.failedFuture(e);
		}

		CompletableFuture<GameMap> mapFuture = phaseDefinition.getMap().open(server);

		BehaviorList behaviors = phaseDefinition.createBehaviors();

		return mapFuture
				.thenApplyAsync(map -> {
					GamePhase phase = new GamePhase(game, gameDefinition, map, behaviors);
					phase.setFocusedLive(game.lobby.metadata.visibility().isFocusedLive());
					queuedGames.add(phase);
					return phase;
				}, server)
				.exceptionally(throwable -> {
					GameException gameException = GameException.unwrap(throwable);
					if (gameException != null) {
						throw new CompletionException(gameException);
					}
					throw new CompletionException(new GameException(Component.literal("An unexpected exception occurred while creating game phase"), throwable));
				});
	}

	void checkCanAddGamePhase(IGamePhaseDefinition definition) throws GameException {
		IGameMapProvider map = definition.getMap();
		for (ResourceKey<Level> dimension : map.getPossibleDimensions()) {
			List<GamePhase> games = gamesByDimension.getOrDefault(dimension, Collections.emptyList());
			if (!games.isEmpty()) {
				throw new GameException(GameTexts.Commands.GAMES_INTERSECT);
			}
		}
	}

	@Nullable
	@Override
	public GamePhase getGamePhaseFor(Player player) {
		GameLobby lobby = GameLobbyManager.get().getLobbyFor(player);
		return lobby != null ? lobby.getActivePhase() : null;
	}

	@Nullable
	@Override
	public GamePhase getGamePhaseAt(Level level, Vec3 pos) {
		List<GamePhase> phases = getGamePhasesForLevel(level);
		return !phases.isEmpty() ? phases.getFirst() : null;
	}

	@Nullable
	@Override
	public IGamePhase getGamePhaseInDimension(Level level) {
		List<GamePhase> games = gamesByDimension.get(level.dimension());
		if (games != null && games.size() == 1) {
			return games.getFirst();
		}
		return null;
	}

	public List<GamePhase> getGamePhasesForLevel(Level level) {
		if (level.isClientSide()) {
			return List.of();
		}
		return gamesByDimension.getOrDefault(level.dimension(), List.of());
	}

	public ControlCommandInvoker getControlInvoker(CommandSourceStack source) {
		GamePhase phase = (GamePhase) getGamePhaseFor(source);
		return phase != null ? phase.controlCommands() : ControlCommandInvoker.EMPTY;
	}

	@SubscribeEvent
	public static void onServerStopping(ServerStoppingEvent event) {
		INSTANCE.onServerStopping();
	}

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Pre event) {
		INSTANCE.onServerTick();
	}

	@SubscribeEvent
	public static void onLevelTick(LevelTickEvent.Post event) {
		if (event.getLevel() instanceof ServerLevel level) {
			INSTANCE.onLevelTick(level);
		}
	}

	private void onServerStopping() {
		for (List<GamePhase> phases : gamesByDimension.values()) {
			phases.forEach(GamePhase::stopForServerShutdown);
		}
		gamesByDimension.clear();
	}

	private void onServerTick() {
		for (GamePhase queuedGame : queuedGames) {
			gamesByDimension.computeIfAbsent(queuedGame.dimension(), d -> new ArrayList<>()).add(queuedGame);
		}
		queuedGames.clear();
	}

	private void onLevelTick(ServerLevel level) {
		List<GamePhase> games = gamesByDimension.get(level.dimension());
		if (games == null) {
			return;
		}
		games.removeIf(GamePhase::tick);
		if (games.isEmpty()) {
			gamesByDimension.remove(level.dimension());
		}
	}
}
