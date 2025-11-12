package com.lovetropics.minigames.common.core.game.impl;

import com.google.common.collect.Lists;
import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.river_race.event.RiverRaceEvents;
import com.lovetropics.minigames.common.core.game.GamePhaseType;
import com.lovetropics.minigames.common.core.game.GameStopReason;
import com.lovetropics.minigames.common.core.game.IGameDefinition;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.IGamePhaseDefinition;
import com.lovetropics.minigames.common.core.game.PlayerIsolation;
import com.lovetropics.minigames.common.core.game.behavior.BehaviorList;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.SubGameEvents;
import com.lovetropics.minigames.common.core.game.config.GameConfig;
import com.lovetropics.minigames.common.core.game.map.GameMap;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

public class MultiGamePhase extends GamePhase {

	private static final Logger LOGGER = LogManager.getLogger(MultiGamePhase.class);

	private final Queue<GameConfig> subPhaseQueue = new ArrayDeque<>();
	@Nullable
	private GamePhase subPhase;

	// TODO: Big hack - can we do something better by splitting what we expose to game impls vs what we expose to the outside?
	//       Some behaviors such as spectator_chase check the spectator list when the player is removed - but that spectator list didn't get the player removed
	private boolean hideRoles;

	protected MultiGamePhase(GameInstance game, IGameDefinition gameDefinition, IGamePhaseDefinition definition, GamePhaseType phaseType, GameMap map, BehaviorList behaviors) {
		super(game, gameDefinition, definition, phaseType, map, behaviors);
	}

	public void startSubPhase(GamePhase subPhase, final boolean saveInventory) {
		this.subPhase = subPhase;
		MultiGameManager.INSTANCE.addGamePhaseToDimension(subPhase.dimension(), subPhase);
		subPhase.assignRolesFrom(this);
		hideRoles = true;
		for (ServerPlayer player : allPlayers()) {
			movePlayerToSubPhase(player);
		}
		hideRoles = false;

		subPhase.events.listen(GamePhaseEvents.CREATE, () ->
				invoker(SubGameEvents.CREATE).onCreateSubGame(subPhase, subPhase.events)
		);
		subPhase.start(saveInventory);
	}

	private void movePlayerToSubPhase(ServerPlayer player) {
		invoker(GamePlayerEvents.REMOVE).onRemove(player);
		addedPlayers.remove(player.getUUID());
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
		addedPlayers.add(player.getUUID());

		PlayerRole role = getRoleFor(player);
		invoker(GamePlayerEvents.RETURN).onReturn(player.getUUID(), role);

		ServerPlayer newPlayer = PlayerIsolation.INSTANCE.reloadPlayerFromMemory(game, player);

		invoker(GamePlayerEvents.ADD).onAdd(newPlayer);
		invoker(GamePlayerEvents.SET_ROLE).onSetRole(newPlayer, role, null);

		return newPlayer;
	}

	@Override
	public GamePhase getActivePhase() {
		return Objects.requireNonNullElse(subPhase, this);
	}

	@Nullable
	@Override
	GameStopReason tick() {
		if (subPhase != null) {
			if (subPhase.tick() != null) {
				GamePhase lastPhase = subPhase;
				startNextQueuedMicrogame(false).whenComplete((newGame, throwable) -> {
					if (throwable != null || !newGame) {
						returnHere(lastPhase);
					}
					if (throwable != null) {
						LOGGER.error("Failed to start next queued micro-game", throwable);
					}
				});
				return null;
			}
			return stopped;
		}
		return super.tick();
	}

	@Override
	ServerPlayer onPlayerJoin(ServerPlayer player, boolean savePlayerDataToMemory) {
		player = super.onPlayerJoin(player, savePlayerDataToMemory);
		if (subPhase != null) {
			// Let the top-level game decide how the player can join, and then just pass them along
			subPhase.assignRolesFrom(this);
			movePlayerToSubPhase(player);
			return subPhase.onPlayerJoin(player, true);
		}
		return player;
	}

	@Override
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
		return super.onPlayerLeave(player, loggingOut);
	}

	private void destroySubGame() {
		if (subPhase != null) {
			subPhase.destroy();
			MultiGameManager.INSTANCE.removeGamePhaseFromDimension(subPhase.dimension(), subPhase);
			subPhase = null;
		}
	}

	@Override
	void destroy() {
		destroySubGame();
		super.destroy();
	}

	@Override
	public PlayerSet getPlayersWithRole(PlayerRole role) {
		if (hideRoles) {
			return PlayerSet.EMPTY;
		}
		return super.getPlayersWithRole(role);
	}

	public void clearQueuedGames() {
		subPhaseQueue.clear();
	}

	public void queueGames(List<GameConfig> games) {
		subPhaseQueue.addAll(games);
	}

	public CompletableFuture<Boolean> startNextQueuedMicrogame(final boolean saveInventory) {
		destroySubGame();
		// No queued games left
		if (subPhaseQueue.isEmpty()) {
			return CompletableFuture.completedFuture(false);
		}
		final GameConfig nextGame = subPhaseQueue.remove();
		return GamePhase.create(game, nextGame, nextGame.getPlayingPhase(), GamePhaseType.PLAYING).thenApply(result -> {
			if (result.isOk()) {
				startSubPhase(result.getOk(), saveInventory);
				invoker(RiverRaceEvents.MICROGAME_STARTED).onMicrogameStarted(this);
				game.allPlayers().sendMessage(Component.literal("Now Playing: ").append(nextGame.name()).withStyle(ChatFormatting.GREEN));
				game.allPlayers().showTitle(Component.empty().append(nextGame.name()).withStyle(ChatFormatting.GREEN),
						nextGame.subtitle(), 10, 40, 10);
				return true;
			}
			LOGGER.error("Failed to start micro-game {} - {}", nextGame.id().toString(), result.getError().getString());
			return false;
		});
	}
}
