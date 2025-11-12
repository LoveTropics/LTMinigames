package com.lovetropics.minigames.common.core.game;

import com.lovetropics.minigames.common.core.game.behavior.event.GameEventListeners;
import com.lovetropics.minigames.common.core.game.behavior.event.GameEventType;
import com.lovetropics.minigames.common.core.game.lobby.GameLobbyMetadata;
import com.lovetropics.minigames.common.core.game.lobby.IGameLobby;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.state.GameStateMap;
import com.lovetropics.minigames.common.core.game.state.control.ControlCommandInvoker;
import com.lovetropics.minigames.common.core.game.state.control.ControlCommands;
import com.lovetropics.minigames.common.core.game.state.statistics.GameStatistics;
import com.lovetropics.minigames.common.core.game.state.statistics.PlayerKey;
import com.lovetropics.minigames.common.core.game.util.GameScheduler;
import com.lovetropics.minigames.common.core.game.util.TeamAllocator;
import com.lovetropics.minigames.common.core.map.MapRegions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

public interface IGamePhase extends IGame {
	IGame game();

	@Override
	default IGameLobby lobby() {
		return game().lobby();
	}

	@Override
	default UUID gameUuid() {
		return game().gameUuid();
	}

	@Override
	default MinecraftServer server() {
		return game().server();
	}

	@Override
	default PlayerKey initiator() {
		return game().initiator();
	}

	@Override
	default PlayerSet allPlayers() {
		return game().allPlayers();
	}

	@Override
	default IGameDefinition definition() {
		return game().definition();
	}

	@Override
	default GameStateMap instanceState() {
		return game().instanceState();
	}

	GameStateMap state();

	GamePhaseType phaseType();

	IGamePhaseDefinition phaseDefinition();

	GameEventListeners events();

	<T> T invoker(GameEventType<T> type);

	GameResult<Unit> requestStop(GameStopReason reason);

	GameScheduler scheduler();

	void allocateRoles(TeamAllocator<PlayerRole, ServerPlayer> allocator);

	/**
	 * Adds the player to this game instance with the given role, or if already in the change, changes their role.
	 * The given player will be removed from their former role, if any.
	 *
	 * @param player the player to add
	 * @param role the role to add the player to
	 * @return whether the player was successfully added or if their role was changed
	 */
	boolean setPlayerRole(ServerPlayer player, @Nullable PlayerRole role);

	/**
	 * @return The list of players within this game instance that belong to the given role
	 */
	PlayerSet getPlayersWithRole(PlayerRole role);

	/**
	 * @return The list of active participants that are playing within the game instance.
	 */
	default PlayerSet participants() {
		return getPlayersWithRole(PlayerRole.PARTICIPANT);
	}

	/**
	 * @return The list of spectators that are observing the game instance.
	 */
	default PlayerSet spectators() {
		return getPlayersWithRole(PlayerRole.SPECTATOR);
	}

	default PlayerSet overlords() {
		return getPlayersWithRole(PlayerRole.OVERLORD);
	}

	@Nullable
	default PlayerRole getRoleFor(ServerPlayer player) {
		for (PlayerRole role : PlayerRole.ROLES) {
			if (getPlayersWithRole(role).contains(player)) {
				return role;
			}
		}
		return null;
	}

	/**
	 * @return the world that this game takes place within
	 */
	ServerLevel level();

	/**
	 * @return the dimension that this game takes places within
	 */
	default ResourceKey<Level> dimension() {
		return level().dimension();
	}

	MapRegions mapRegions();

	/**
	 * @return the tick counter since the game started
	 */
	long ticks();

	default RandomSource random() {
		return level().getRandom();
	}

	default ControlCommands controlCommands() {
		return state().get(ControlCommands.KEY);
	}

	default GameStatistics statistics() {
		return state().get(GameStatistics.KEY);
	}

	default ControlCommandInvoker getControlInvoker() {
		ControlCommands commands = controlCommands();
		return ControlCommandInvoker.create(commands);
	}

	default IGamePhase getTopPhase() {
		return Objects.requireNonNullElse(lobby().getTopPhase(), this);
	}
}
