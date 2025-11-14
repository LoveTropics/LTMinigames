package com.lovetropics.minigames.common.core.game.impl;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.GameResult;
import com.lovetropics.minigames.common.core.game.IGameLookup;
import com.lovetropics.minigames.common.core.game.IGamePhaseDefinition;
import com.lovetropics.minigames.common.core.game.map.IGameMapProvider;
import com.lovetropics.minigames.common.core.game.state.control.ControlCommandInvoker;
import com.lovetropics.minigames.common.core.game.util.GameTexts;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = LoveTropics.ID)
public class GamePhaseManager implements IGameLookup {
	private static final GamePhaseManager INSTANCE = new GamePhaseManager();

	private final Map<ResourceKey<Level>, List<GamePhase>> gamesByDimension = new Reference2ObjectOpenHashMap<>();

	public static GamePhaseManager get() {
		return GamePhaseManager.INSTANCE;
	}

	GameResult<Unit> canStartGamePhase(IGamePhaseDefinition definition) {
		IGameMapProvider map = definition.getMap();
		List<ResourceKey<Level>> possibleDimensions = map.getPossibleDimensions();

		for (ResourceKey<Level> dimension : possibleDimensions) {
			List<GamePhase> games = gamesByDimension.getOrDefault(dimension, Collections.emptyList());
			if (!games.isEmpty()) {
				return GameResult.error(GameTexts.Commands.GAMES_INTERSECT);
			}
		}

		return GameResult.ok();
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
		return getGamePhaseInDimension(level);
	}

	@Nullable
	@Override
	public GamePhase getGamePhaseInDimension(Level level) {
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
		return gamesByDimension.getOrDefault(level.dimension(), Collections.emptyList());
	}

	public ControlCommandInvoker getControlInvoker(CommandSourceStack source) {
		GamePhase phase = (GamePhase) getGamePhaseFor(source);
		return phase != null ? phase.controlCommands() : ControlCommandInvoker.EMPTY;
	}

	void addGamePhaseToDimension(ResourceKey<Level> dimension, GamePhase game) {
		gamesByDimension.computeIfAbsent(dimension, k -> new ArrayList<>())
				.add(game);
	}

	void removeGamePhaseFromDimension(ResourceKey<Level> dimension, GamePhase game) {
		List<GamePhase> games = gamesByDimension.get(dimension);
		if (games == null) {
			return;
		}

		if (games.remove(game) && games.isEmpty()) {
			gamesByDimension.remove(dimension, games);
		}
	}

	@SubscribeEvent
	public static void onPlayerTryChangeDimension(EntityTravelToDimensionEvent event) {
		Entity entity = event.getEntity();
		if (entity instanceof ServerPlayer player) {
			ServerLevel targetWorld = player.getServer().getLevel(event.getDimension());
			if (targetWorld == null) {
				return;
			}

			GamePhase playerPhase = INSTANCE.getGamePhaseFor(player);
			GamePhase targetPhase = (GamePhase) INSTANCE.getGamePhaseAt(targetWorld, player.blockPosition());
			if (!canTravelBetweenPhases(playerPhase, targetPhase)) {
				player.displayClientMessage(GameTexts.Commands.cannotTeleportIntoGame(), true);

				event.setCanceled(true);
			}
		}
	}

	private static boolean canTravelBetweenPhases(@Nullable GamePhase from, @Nullable GamePhase to) {
		if (to == null) {
			return true;
		} else if (from == null) {
			return false;
		}
		return from.game.lobby == to.game.lobby;
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			GamePhase phase = INSTANCE.getGamePhaseFor(player);
			if (phase == null) {
				return;
			}

			ResourceKey<Level> dimension = phase.dimension();
			if (event.getFrom() == dimension && event.getTo() != dimension) {
				if (phase.game.lobby.getPlayers().remove(player, false)) {
					player.displayClientMessage(GameTexts.Status.leftGameDimension(), false);
				}
			}
		}
	}
}
