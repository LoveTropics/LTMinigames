package com.lovetropics.minigames.common.core.game.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public final class TeamAllocator<T, V> {
	private final List<T> teams;
	private final List<V> players = new ArrayList<>();
	private final Map<V, T> teamPreferences = new Object2ObjectOpenHashMap<>();

	private final Object2IntMap<T> teamSizes = new Object2IntOpenHashMap<>();
	private @Nullable T overflowTeam;
	private final List<T> lockedTeams = new ArrayList<>();

	public TeamAllocator(Collection<T> teams) {
		Preconditions.checkArgument(!teams.isEmpty(), "cannot allocate with no teams");

		this.teams = new ArrayList<>(teams);
		teamSizes.defaultReturnValue(Integer.MAX_VALUE);
	}

	public void setSizeForTeam(T team, int maxSize) {
		Preconditions.checkArgument(teams.contains(team), "invalid team: " + team);
		Preconditions.checkArgument(maxSize > 0, "max team size must be >0");

		teamSizes.put(team, maxSize);
	}

	public void setOverflowTeam(T team) {
		Preconditions.checkArgument(teams.contains(team), "invalid team: " + team);
		overflowTeam = team;
	}

	public void addLockedTeam(T team) {
		Preconditions.checkArgument(teams.contains(team), "invalid team: " + team);
		lockedTeams.add(team);
	}

	public Collection<V> getKnownPlayers() {
		return Collections.unmodifiableCollection(players);
	}

	public void addPlayer(V player, @Nullable T preference) {
		if (!players.contains(player)) {
			players.add(player);
		}

		if (preference != null) {
			teamPreferences.put(player, preference);
		}
	}

	public boolean hasPreference(V player) {
		return teamPreferences.containsKey(player);
	}

	public void allocate(BiConsumer<V, T> apply) {
		Multimap<T, V> teamToPlayers = allocate();
		teamToPlayers.forEach((team, player) -> apply.accept(player, team));
	}

	public Multimap<T, V> allocate() {
		Allocations<T, V> allocations = new Allocations<>();
		placePlayersRandomly(allocations);
		optimizeTeamsByPreference(allocations);

		return allocations.teamToPlayers;
	}

	private void placePlayersRandomly(Allocations<T, V> allocations) {
		T overflowTeam = this.overflowTeam;

		List<T> availableTeams = new ArrayList<>(teams);
		if (overflowTeam != null) {
			availableTeams.remove(overflowTeam);
		}
		if (!lockedTeams.isEmpty()) {
			availableTeams.removeAll(lockedTeams);
		}

		List<V> players = new ArrayList<>(this.players);

		// shuffle the player and teams list for random initial allocation
		Collections.shuffle(availableTeams);
		Collections.shuffle(players);

		int teamIndex = 0;
		for (V player : players) {
			if (!availableTeams.isEmpty()) {
				// we still have team space that we can allocate into
				T team = availableTeams.get(teamIndex);
				allocations.setTeam(player, team);

				// check for the maximum team size being exceeded
				int maxTeamSize = teamSizes.getInt(team);
				if (allocations.playersIn(team).size() >= maxTeamSize) {
					// we've reached the maximum size for this team; exclude it from further consideration
					availableTeams.remove(teamIndex);
					continue;
				}

				teamIndex = (teamIndex + 1) % availableTeams.size();
			} else {
				// all teams are full! spill into overflow or error with no more space
				if (overflowTeam != null) {
					allocations.setTeam(player, overflowTeam);
				} else {
					throw new IllegalStateException("team overflow! all teams have exceeded maximum capacity");
				}
			}
		}
	}

	private void optimizeTeamsByPreference(Allocations<T, V> allocations) {
		List<V> players = new ArrayList<>(this.players);
		Collections.shuffle(players);

		// TODO: this algorithm needs another look at it
		for (V player : players) {
			T preference = teamPreferences.get(player);
			T current = allocations.teamFor(player);

			// we have no preference or we are already in our desired position, continue
			if (preference == null || current == preference) {
				continue;
			}

			Collection<V> currentPlayers = allocations.playersIn(current);
			Collection<V> preferencePlayers = allocations.playersIn(preference);

			// we can move without swapping if the other team is smaller than ours if it has not exceeded the max size
			// we only care about keeping the teams balanced, so this is safe
			if (!trySwapWithOtherPlayer(allocations, player, current, preference)) {
				// we couldn't swap with someone else, just move us over
				if (preferencePlayers.size() < currentPlayers.size() && canTeamGrow(preference, preferencePlayers.size())) {
					allocations.moveTeam(player, current, preference);
				}
			}
		}
	}

	private boolean canTeamGrow(T team, int size) {
		int maxSize = teamSizes.getInt(team);
		return size < maxSize;
	}

	private boolean trySwapWithOtherPlayer(Allocations<T, V> allocations, V player, T from, T to) {
		V swapWith = findSwapCandidate(from, to, allocations.playersIn(to));
		if (swapWith != null) {
			allocations.moveTeam(player, from, to);
			allocations.moveTeam(swapWith, to, from);
			return true;
		} else {
			return false;
		}
	}

	private @Nullable V findSwapCandidate(T from, T to, Collection<V> candidates) {
		V swapWith = null;

		for (V candidate : candidates) {
			T candidatePreference = teamPreferences.get(candidate);
			if (candidatePreference == to) {
				// we can't swap with this player: they are already in their chosen team
				continue;
			}

			// prioritise players who want to join our current team
			if (swapWith == null || candidatePreference == from) {
				swapWith = candidate;
			}
		}

		return swapWith;
	}

	static final class Allocations<T, V> {
		final Multimap<T, V> teamToPlayers = HashMultimap.create();
		final Map<V, T> playerToTeam = new Object2ObjectOpenHashMap<>();

		void setTeam(V player, T team) {
			teamToPlayers.put(team, player);
			playerToTeam.put(player, team);
		}

		T teamFor(V player) {
			return playerToTeam.get(player);
		}

		Collection<V> playersIn(T team) {
			return teamToPlayers.get(team);
		}

		void moveTeam(V player, T from, T to) {
			teamToPlayers.remove(from, player);
			teamToPlayers.put(to, player);
			playerToTeam.put(player, to);
		}
	}
}
