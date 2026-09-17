package com.lovetropics.minigames.common.core.game.impl;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGameDefinition;
import com.lovetropics.minigames.common.core.game.IGameLookup;
import com.lovetropics.minigames.common.core.game.IGamePhaseDefinition;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.map.GameMap;
import com.lovetropics.minigames.common.core.game.map.IGameMapProvider;
import com.lovetropics.minigames.common.core.game.util.GameTexts;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import org.jspecify.annotations.Nullable;
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

	public CompletableFuture<GamePhase> createTopPhase(GameInstance game, IGamePhaseDefinition phaseDefinition) {
		return createPhase(game, null, game.definition(), phaseDefinition);
	}

	public CompletableFuture<GamePhase> createSubPhase(GamePhase parentPhase, IGameDefinition subGameDefinition) {
		return createPhase(parentPhase.game, parentPhase, subGameDefinition, subGameDefinition.getPlayingPhase());
	}

	private CompletableFuture<GamePhase> createPhase(GameInstance game, @Nullable GamePhase parentPhase, IGameDefinition definition, IGamePhaseDefinition phaseDefinition) {
		try {
			checkCanAddGamePhase(phaseDefinition);
		} catch (GameException e) {
			return CompletableFuture.failedFuture(e);
		}

		CompletableFuture<GameMap> mapFuture = phaseDefinition.getMap().open(game.server());

		IGameBehavior behavior = phaseDefinition.createBehavior();

		return mapFuture
				.thenApplyAsync(map -> {
					// TODO: Rather have the async CompletableFuture part only prepare the map - create the GamePhase only from the outside
					GamePhase phase = new GamePhase(game, parentPhase, map, definition, behavior);
					queuedGames.add(phase);
					return phase;
				}, game.server())
				.exceptionally(throwable -> {
					GameException gameException = GameException.unwrap(throwable);
					if (gameException != null) {
						throw new CompletionException(gameException);
					}
					throw new CompletionException(new GameException(Component.literal("An unexpected exception occurred while creating game phase ").append(throwable.getMessage()), throwable)); // Todo I added the throwable message here, error does not seem to passed all the way done so I just did this for now - UnReal
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

	@Override
	public @Nullable GamePhase getGamePhaseFor(Player player) {
		return getGamePhaseInDimension(player.level());
	}

	@Override
	public @Nullable GamePhase getGamePhaseAt(Level level, Vec3 pos) {
		return getGamePhaseInDimension(level);
	}

	@Override
	public @Nullable GamePhase getGamePhaseInDimension(Level level) {
		if (level.isClientSide()) {
			return null;
		}
		List<GamePhase> games = gamesByDimension.get(level.dimension());
		if (games != null && games.size() == 1) {
			return games.getFirst();
		}
		return null;
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
		gamesByDimension.values().stream()
				.flatMap(List::stream)
				.distinct()
				.toList()
				.forEach(GamePhase::stopForServerShutdown);
		gamesByDimension.clear();
	}

	private void onServerTick() {
		for (GamePhase queuedGame : queuedGames) {
			for (ResourceKey<Level> dimension : queuedGame.dimensions()) {
				gamesByDimension.computeIfAbsent(dimension, d -> new ArrayList<>()).add(queuedGame);
			}
		}
		queuedGames.clear();
	}

	private void onLevelTick(ServerLevel level) {
		List<GamePhase> games = gamesByDimension.get(level.dimension());
		if (games == null) {
			return;
		}
		List<GamePhase> destroyedGames = new ArrayList<>(0);
		for (GamePhase game : games) {
			// Games spanning several dimensions only tick along with their main one
			if (game.dimension() == level.dimension() && game.tick()) {
				destroyedGames.add(game);
			}
		}
		for (GamePhase game : destroyedGames) {
			for (ResourceKey<Level> dimension : game.dimensions()) {
				List<GamePhase> dimensionGames = gamesByDimension.get(dimension);
				if (dimensionGames != null && dimensionGames.remove(game) && dimensionGames.isEmpty()) {
					gamesByDimension.remove(dimension);
				}
			}
		}
	}
}
