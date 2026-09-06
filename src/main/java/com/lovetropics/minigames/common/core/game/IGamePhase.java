package com.lovetropics.minigames.common.core.game;

import com.lovetropics.minigames.common.core.game.behavior.event.GameEventType;
import com.lovetropics.minigames.common.core.game.player.PlayerIterable;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.state.GameStateMap;
import com.lovetropics.minigames.common.core.game.state.statistics.GameStatistics;
import com.lovetropics.minigames.common.core.game.util.GameScheduler;
import com.lovetropics.minigames.common.core.integration.GameInstanceIntegrations;
import com.lovetropics.minigames.common.core.map.MapRegions;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.level.Level;

import org.jspecify.annotations.Nullable;

public interface IGamePhase {
	/// @return the world that this game takes place within
	ServerLevel level();

	MapRegions mapRegions();

	/// @return the dimension that this game takes places within
	default ResourceKey<Level> dimension() {
		return level().dimension();
	}

	default RandomSource random() {
		return level().getRandom();
	}

	default MinecraftServer server() {
		return level().getServer();
	}

	default RegistryAccess registryAccess() {
		return level().registryAccess();
	}

	default PlayerSet allPlayers() {
		return allPlayers(false);
	}

	IGameDefinition definition();

	GameStateMap state();

	GameStateMap instanceState();

	<T> T invoker(GameEventType<T> type);

	GameResult<Unit> requestStop(GameStopReason reason);

	PendingSubPhase createSubPhase(IGameDefinition subGameConfig);

	void returnToParent(ServerPlayer player);

	PlayerSet allPlayers(boolean includeSubPhases);

	default void returnToParent(PlayerIterable players) {
		players.forEach(this::returnToParent);
	}

	void transferPlayerTo(ServerPlayer player, IGamePhase subPhase);

	default void transferPlayersTo(PlayerIterable players, IGamePhase subPhase) {
		for (ServerPlayer player : players) {
			transferPlayerTo(player, subPhase);
		}
	}

	GameScheduler scheduler();

	/// Adds the player to this game instance with the given role, or if already in the change, changes their role.
	/// The given player will be removed from their former role, if any.
	///
	/// @param player the player to add
	/// @param role the role to add the player to
	/// @return whether the player was successfully added or if their role was changed
	boolean setPlayerRole(ServerPlayer player, @Nullable PlayerRole role);

	/// @return The list of players within this game instance that belong to the given role
	PlayerSet getPlayersWithRole(PlayerRole role);

	/// @return The list of active participants that are playing within the game instance.
	default PlayerSet participants() {
		return getPlayersWithRole(PlayerRole.PARTICIPANT);
	}

	/// @return The list of spectators that are observing the game instance.
	default PlayerSet spectators() {
		return getPlayersWithRole(PlayerRole.SPECTATOR);
	}

	@Nullable PlayerRole getRoleFor(ServerPlayer player);

	/// @return the tick counter since the game started
	long ticks();

	default GameStatistics statistics() {
		return state().get(GameStatistics.KEY);
	}

	default GameInstanceIntegrations getIntegrationsOrThrow() {
		return instanceState().getOrThrow(GameInstanceIntegrations.KEY);
	}

	boolean isFocusedLive();
}
