package org.lovetropics.games.common.core.game.state.team;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.player.PlayerSet;
import org.lovetropics.games.common.core.game.state.GameStateKey;
import org.lovetropics.games.common.core.game.state.IGameState;
import org.lovetropics.games.common.core.game.state.statistics.PlayerKey;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class TeamState implements IGameState, Iterable<GameTeam> {
	public static final GameStateKey<TeamState> KEY = GameStateKey.create("Teams");

	public static final TeamState EMPTY = new TeamState(List.of());

	private final List<GameTeam> teams;

	private final Object2ObjectMap<GameTeamKey, GameTeam> teamsByKey = new Object2ObjectOpenHashMap<>();
	private final Object2ObjectMap<GameTeamKey, Set<UUID>> playersByKey = new Object2ObjectOpenHashMap<>();

	public TeamState(List<GameTeam> teams) {
		this.teams = teams;
		for (GameTeam team : teams) {
			teamsByKey.put(team.key(), team);
		}
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

	public @Nullable GameTeamKey removePlayer(UUID playerId) {
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

	public @Nullable GameTeamKey getTeamForPlayer(Player player) {
		return getTeamForPlayer(player.getUUID());
	}

	public @Nullable GameTeamKey getTeamForPlayer(PlayerKey player) {
		return getTeamForPlayer(player.id());
	}

	public @Nullable GameTeamKey getTeamForPlayer(UUID playerId) {
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

	public @Nullable GameTeam getTeamByKey(String key) {
		for (GameTeam team : teams) {
			if (team.key().id().equals(key)) {
				return team;
			}
		}
		return null;
	}

	public @Nullable GameTeam getTeamByKey(GameTeamKey key) {
		return teamsByKey.get(key);
	}

	public GameTeam getTeamOrThrow(GameTeamKey key) {
		return Objects.requireNonNull(getTeamByKey(key));
	}

	public int getAssignedTeamSize(GameTeamKey key) {
		Set<UUID> players = playersByKey.get(key);
		return players != null ? players.size() : 0;
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
}
