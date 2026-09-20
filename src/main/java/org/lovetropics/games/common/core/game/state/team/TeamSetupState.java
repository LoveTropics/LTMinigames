package org.lovetropics.games.common.core.game.state.team;

import net.minecraft.server.level.ServerPlayer;
import org.lovetropics.games.common.core.game.state.GameStateKey;
import org.lovetropics.games.common.core.game.state.IGameState;
import org.lovetropics.games.common.core.game.state.statistics.PlayerKey;
import org.lovetropics.games.common.core.game.util.TeamAllocator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class TeamSetupState implements IGameState {
	public static final GameStateKey<TeamSetupState> KEY = GameStateKey.create("Team Setup");

	private final Map<GameTeamKey, Instance> teams = new HashMap<>();
	private final Map<PlayerKey, GameTeamKey> assignments = new HashMap<>();
	private final Map<PlayerKey, GameTeamKey> preferences = new HashMap<>();

	public TeamState createInitialTeamState() {
		return new TeamState(teams.values().stream().map(i -> i.team).toList());
	}

	public void allocatePlayers(TeamState teams, Set<PlayerKey> participants) {
		for (Map.Entry<PlayerKey, GameTeamKey> entry : assignments.entrySet()) {
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
		List<GameTeamKey> openTeamKeys = openTeams.stream().map(i -> i.team.key()).toList();
		TeamAllocator<GameTeamKey, PlayerKey> teamAllocator = new TeamAllocator<>(openTeamKeys);

		for (Instance instance : openTeams) {
			int assignedPlayers = teams.getAssignedTeamSize(instance.team.key());
			teamAllocator.setSizeForTeam(instance.team.key(), instance.maxPlayers - assignedPlayers);
		}

		for (PlayerKey player : participants) {
			if (teams.getTeamForPlayer(player) == null) {
				teamAllocator.addPlayer(player, preferences.get(player));
			}
		}

		teamAllocator.allocate(teams::addPlayerTo);
	}

	public Instance addTeam(GameTeam team) {
		Instance instance = new Instance(team);
		teams.put(team.key(), instance);
		return instance;
	}

	private void validateTeam(GameTeamKey team) {
		if (!teams.containsKey(team)) {
			throw new IllegalArgumentException("Team " + team + " does not exist");
		}
	}

	public void assignPlayer(ServerPlayer player, GameTeamKey team) {
		validateTeam(team);
		assignments.put(PlayerKey.from(player), team);
	}

	public void setPlayerPreference(ServerPlayer player, GameTeamKey team) {
		validateTeam(team);
		preferences.put(PlayerKey.from(player), team);
	}

	public void removePlayer(ServerPlayer player) {
		PlayerKey key = PlayerKey.from(player);
		assignments.remove(key);
		preferences.remove(key);
	}

	public Stream<PlayerKey> assignedPlayers() {
		return assignments.keySet().stream();
	}

	public static class Instance {
		private final GameTeam team;
		private int maxPlayers = Integer.MAX_VALUE;
		private boolean openToJoin = true;

		private Instance(GameTeam team) {
			this.team = team;
		}

		public void setMaxPlayers(int maxPlayers) {
			this.maxPlayers = maxPlayers;
		}

		public void setOpenToJoin(boolean openToJoin) {
			this.openToJoin = openToJoin;
		}
	}
}
