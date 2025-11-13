package com.lovetropics.minigames.common.core.game.state.team;

import com.google.common.base.Preconditions;
import com.lovetropics.lib.permission.PermissionsApi;
import com.lovetropics.lib.permission.role.Role;
import com.lovetropics.lib.permission.role.RoleReader;
import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.state.GameStateKey;
import com.lovetropics.minigames.common.core.game.state.IGameState;
import com.lovetropics.minigames.common.core.game.state.statistics.PlayerKey;
import com.lovetropics.minigames.common.core.game.util.TeamAllocator;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class TeamState implements IGameState, Iterable<GameTeam> {
	public static final GameStateKey<TeamState> KEY = GameStateKey.create("Teams");

	public static final TeamState EMPTY = new TeamState(List.of());

	private final List<GameTeam> teams;

	private final Object2ObjectMap<GameTeamKey, GameTeam> teamsByKey = new Object2ObjectOpenHashMap<>();
	private final Object2ObjectMap<GameTeamKey, Set<UUID>> playersByKey = new Object2ObjectOpenHashMap<>();

	private final Collection<GameTeam> pollingTeams;

	@Nullable
	private Allocations allocations = new Allocations();

	public TeamState(List<GameTeam> teams) {
		this.teams = teams;

		pollingTeams = new ObjectOpenHashSet<>();

		for (GameTeam team : teams) {
			teamsByKey.put(team.key(), team);
			if (team.config().assignedRoles().isEmpty()) {
				pollingTeams.add(team);
			}
		}
	}

	public void setPlayerPreference(UUID player, GameTeamKey team) {
		if (allocations != null) {
			allocations.setPlayerPreference(player, team);
		}
	}

	public void allocatePlayers(Set<PlayerKey> participants) {
		// We might end up here in a microgame, so don't reassign teams if they were already decided
		if (allocations == null) {
			return;
		}
		allocations.allocate(participants, (player, teamKey) -> {
			GameTeam team = getTeamByKey(teamKey);
			if (team != null) {
				addPlayerTo(player, teamKey);
			}
		});
		allocations = null;
	}

	public void addPlayerTo(PlayerKey player, GameTeamKey team) {
		removePlayer(player.id());

		Set<UUID> players = playersByKey.get(team);
		if (players == null) {
			Preconditions.checkState(teams.contains(getTeamByKey(team)), "invalid team " + team);
			players = new ObjectOpenHashSet<>();
			playersByKey.put(team, players);
		}
		players.add(player.id());
	}

	@Nullable
	public GameTeamKey removePlayer(UUID playerId) {
		for (Map.Entry<GameTeamKey, Set<UUID>> entry : Object2ObjectMaps.fastIterable(playersByKey)) {
			if (entry.getValue().remove(playerId)) {
				return entry.getKey();
			}
		}

		return null;
	}

	public PlayerSet getParticipantsForTeam(IGamePhase game, GameTeamKey team) {
		Set<UUID> players = playersByKey.get(team);
		if (players == null) {
			return PlayerSet.EMPTY;
		}
		return game.participants().filter(player -> players.contains(player.getUUID()));
	}

	public PlayerSet getPlayersForTeam(IGamePhase game, GameTeamKey team) {
		Set<UUID> players = playersByKey.get(team);
		if (players == null) {
			return PlayerSet.EMPTY;
		}
		return game.allPlayers().filter(player -> players.contains(player.getUUID()));
	}

	public PlayerSet getPlayersOnSameTeam(IGamePhase game, ServerPlayer player) {
		GameTeamKey team = getTeamForPlayer(player);
		if (team == null) {
			return PlayerSet.of(player);
		}
		return getPlayersForTeam(game, team);
	}

	@Nullable
	public GameTeamKey getTeamForPlayer(Player player) {
		return getTeamForPlayer(player.getUUID());
	}

	@Nullable
	public GameTeamKey getTeamForPlayer(PlayerKey player) {
		return getTeamForPlayer(player.id());
	}

	@Nullable
	public GameTeamKey getTeamForPlayer(UUID playerId) {
		for (Map.Entry<GameTeamKey, Set<UUID>> entry : Object2ObjectMaps.fastIterable(playersByKey)) {
			if (entry.getValue().contains(playerId)) {
				return entry.getKey();
			}
		}
		return null;
	}

	public boolean isOnTeam(Player player, GameTeamKey team) {
		Set<UUID> players = playersByKey.get(team);
		return players != null && players.contains(player.getUUID());
	}

	public Collection<GameTeamKey> getTeamKeys() {
		return teamsByKey.keySet();
	}

	public Collection<GameTeam> getPollingTeams() {
		return pollingTeams;
	}

	public List<Role> assignedRoles() {
		return teams.stream().flatMap(TeamState::assignedRoles).distinct().toList();
	}

	@Nullable
	public GameTeam getTeamByKey(String key) {
		for (GameTeam team : teams) {
			if (team.key().id().equals(key)) {
				return team;
			}
		}
		return null;
	}

	@Nullable
	public GameTeam getTeamByKey(GameTeamKey key) {
		return teamsByKey.get(key);
	}

	public GameTeam getTeamOrThrow(GameTeamKey key) {
		return Objects.requireNonNull(getTeamByKey(key));
	}

	public boolean areSameTeam(Entity source, Entity target) {
		if (!(source instanceof Player) || !(target instanceof Player)) {
			return false;
		}
		GameTeamKey sourceTeam = getTeamForPlayer((Player) source);
		GameTeamKey targetTeam = getTeamForPlayer((Player) target);
		return Objects.equals(sourceTeam, targetTeam);
	}

	@Override
	public Iterator<GameTeam> iterator() {
		return teams.iterator();
	}

	public int size() {
		return teams.size();
	}

	public final class Allocations {
		private final Map<UUID, GameTeamKey> preferences = new Object2ObjectOpenHashMap<>();

		public void allocate(Set<PlayerKey> participants, BiConsumer<PlayerKey, GameTeamKey> apply) {
			// apply all direct team assignments first
			Set<UUID> assignedPlayers = new ObjectOpenHashSet<>();
			for (GameTeam team : teams) {
				List<Role> assignedRoles = assignedRoles(team).toList();
				for (PlayerKey player : participants) {
					RoleReader roles = PermissionsApi.lookup().byPlayerId(player.id());
					if (assignedRoles.stream().noneMatch(roles::has)) {
						continue;
					}
					LoveTropics.LOGGER.debug("Assigning {} to {} based on role assignments", player.name(), team);
					apply.accept(player, team.key());
					assignedPlayers.add(player.id());
				}
			}

			if (!pollingTeams.isEmpty()) {
				TeamAllocator<GameTeamKey, PlayerKey> teamAllocator = createAllocator();

				for (PlayerKey player : participants) {
					UUID playerId = player.id();
					if (!assignedPlayers.contains(playerId)) {
						teamAllocator.addPlayer(player, preferences.get(playerId));
					}
				}

				teamAllocator.allocate(apply);
			}
		}

		private TeamAllocator<GameTeamKey, PlayerKey> createAllocator() {
			List<GameTeamKey> pollingTeamKeys = pollingTeams.stream().map(GameTeam::key).collect(Collectors.toList());
			TeamAllocator<GameTeamKey, PlayerKey> teamAllocator = new TeamAllocator<>(pollingTeamKeys);

			for (GameTeam team : pollingTeams) {
				teamAllocator.setSizeForTeam(team.key(), team.config().maxSize());
			}

			return teamAllocator;
		}

		public void setPlayerPreference(UUID player, GameTeamKey team) {
			preferences.put(player, team);
		}
	}

	private static Stream<Role> assignedRoles(GameTeam team) {
		return team.config().assignedRoles().stream()
				.map(PermissionsApi.provider()::get)
				.filter(Objects::nonNull);
	}
}
