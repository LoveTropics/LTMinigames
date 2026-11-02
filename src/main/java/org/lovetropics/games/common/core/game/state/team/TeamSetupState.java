package org.lovetropics.games.common.core.game.state.team;

import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.state.GameStateKey;
import org.lovetropics.games.common.core.game.state.IGameState;
import org.lovetropics.games.common.core.game.state.statistics.PlayerKey;
import org.lovetropics.games.common.core.game.util.TeamAllocator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TeamSetupState implements IGameState {
	public static final GameStateKey<TeamSetupState> KEY = GameStateKey.create("Team Setup");

	private final Map<GameTeamKey, Instance> teams = new HashMap<>();
	// Warning: might contain stale teams, this is intentional such that a team being added/removed preserves assignments
	private final Map<PlayerKey, GameTeamKey> assignments = new HashMap<>();
	private final Map<PlayerKey, GameTeamKey> preferences = new HashMap<>();

	public TeamState createInitialTeamState() {
		return new TeamState(teams.values().stream().map(i -> i.team).toList());
	}

	public void allocatePlayers(TeamState teams, Set<PlayerKey> participants) {
		for (Map.Entry<PlayerKey, GameTeamKey> entry : assignments.entrySet()) {
			if (!this.teams.containsKey(entry.getValue())) {
				continue;
			}
			// Ensure we don't hold any assignments for players that don't end up part of the game
			if (!participants.contains(entry.getKey())) {
				continue;
			}
			teams.addPlayerTo(entry.getKey(), entry.getValue());
		}

		List<Instance> openTeams = this.teams.values().stream().filter(i -> i.openToJoin).toList();
		if (!openTeams.isEmpty()) {
			allocateToOpenTeams(teams, participants, openTeams);
		}
	}

	private void allocateToOpenTeams(TeamState teams, Set<PlayerKey> participants, List<Instance> openTeams) {
		Set<GameTeamKey> openTeamKeys = openTeams.stream().map(i -> i.team.key()).collect(Collectors.toSet());
		TeamAllocator<GameTeamKey, PlayerKey> teamAllocator = new TeamAllocator<>(openTeamKeys);

		for (Instance instance : openTeams) {
			int assignedPlayers = teams.getAssignedTeamSize(instance.team.key());
			teamAllocator.setSizeForTeam(instance.team.key(), instance.maxPlayers - assignedPlayers);
		}

		for (PlayerKey player : participants) {
			if (teams.getTeamForPlayer(player) != null) {
				continue;
			}
			GameTeamKey preference = preferences.get(player);
			// Team might have been closed since the player requested joining, ensure to filter it out
			if (preference != null && !openTeamKeys.contains(preference)) {
				preference = null;
			}
			teamAllocator.addPlayer(player, preference);
		}

		teamAllocator.allocate(teams::addPlayerTo);
	}

	public Instance addTeam(GameTeam team) {
		Instance instance = new Instance(team);
		teams.put(team.key(), instance);
		return instance;
	}

	public @Nullable Instance getTeam(GameTeamKey team) {
		return teams.get(team);
	}

	public void removeTeam(GameTeamKey team) {
		teams.remove(team);
	}

	public void assignPlayer(PlayerKey player, GameTeamKey team) {
		assignments.put(player, team);
	}

	public void setPlayerPreference(ServerPlayer player, GameTeamKey team) {
		preferences.put(PlayerKey.from(player), team);
	}

	public void removePlayer(PlayerKey player) {
		assignments.remove(player);
		preferences.remove(player);
	}

	public Stream<PlayerKey> playersAssignedTo(GameTeamKey team) {
		return assignments.entrySet().stream()
				.filter(e -> e.getValue().equals(team))
				.map(Map.Entry::getKey);
	}

	public Stream<PlayerKey> playersWithPreferenceFor(GameTeamKey team) {
		Instance instance = getTeam(team);
		if (instance != null && !instance.openToJoin) {
			return Stream.empty();
		}
		return preferences.entrySet().stream()
				.filter(e -> e.getValue().equals(team))
				.map(Map.Entry::getKey);
	}

	public Stream<PlayerKey> assignedPlayers() {
		return assignments.keySet().stream();
	}

	public Stream<Instance> teamsStream() {
		return teams.values().stream();
	}

	public static class Instance {
		private final GameTeam team;
		private int maxPlayers = Integer.MAX_VALUE;
		private boolean openToJoin = true;

		private Instance(GameTeam team) {
			this.team = team;
		}

		public GameTeamKey key() {
			return team.key();
		}

		public GameTeam team() {
			return team;
		}

		public void setMaxPlayers(int maxPlayers) {
			this.maxPlayers = maxPlayers;
		}

		public void setOpenToJoin(boolean openToJoin) {
			this.openToJoin = openToJoin;
		}

		public boolean isOpenToJoin() {
			return openToJoin;
		}

		public int getMaxPlayers() {
			return maxPlayers;
		}
	}
}
