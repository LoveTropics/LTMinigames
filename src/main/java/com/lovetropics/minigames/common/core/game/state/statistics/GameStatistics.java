package com.lovetropics.minigames.common.core.game.state.statistics;

import com.lovetropics.minigames.common.core.game.state.GameStateKey;
import com.lovetropics.minigames.common.core.game.state.IGameState;
import com.lovetropics.minigames.common.core.game.state.team.GameTeam;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public final class GameStatistics implements IGameState {
	public static final GameStateKey.Defaulted<GameStatistics> KEY = GameStateKey.create("Game Statistics", GameStatistics::new);

	public static final Codec<GameStatistics> CODEC = RecordCodecBuilder.create(i -> i.group(
			StatisticsMap.CODEC.fieldOf("global").forGetter(GameStatistics::global),
			PlayerEntry.CODEC.listOf().fieldOf("players").forGetter(statistics -> statistics.byPlayer.entrySet().stream()
					.map(entry -> new PlayerEntry(entry.getKey(), entry.getValue()))
					.toList()),
			TeamEntry.CODEC.listOf().fieldOf("teams").forGetter(statistics -> statistics.byTeam.entrySet().stream()
					.map(entry -> new TeamEntry(entry.getKey(), entry.getValue()))
					.toList())
	).apply(i, (global, players, teams) -> {
		GameStatistics statistics = new GameStatistics();
		statistics.global.copyFrom(global);
		for (PlayerEntry player : players) {
			statistics.byPlayer.put(player.profile, player.statistics);
		}
		for (TeamEntry team : teams) {
			statistics.byTeam.put(team.id, team.statistics);
		}
		return statistics;
	}));

	private final StatisticsMap global = new StatisticsMap();
	private final Map<PlayerKey, StatisticsMap> byPlayer = new Object2ObjectOpenHashMap<>();
	private final Map<GameTeamKey, StatisticsMap> byTeam = new Object2ObjectOpenHashMap<>();

	public StatisticsMap global() {
		return global;
	}

	public StatisticsMap forPlayer(PlayerKey key) {
		return byPlayer.computeIfAbsent(key, p -> new StatisticsMap());
	}

	public StatisticsMap forPlayer(Player player) {
		return forPlayer(PlayerKey.from(player));
	}

	public StatisticsMap forTeam(GameTeamKey key) {
		return byTeam.computeIfAbsent(key, k -> new StatisticsMap());
	}

	public StatisticsMap forTeam(GameTeam team) {
		return forTeam(team.key());
	}

	public Set<PlayerKey> getPlayers() {
		return byPlayer.keySet();
	}

	public Set<GameTeamKey> getTeams() {
		return byTeam.keySet();
	}

	public void clear(StatisticKey<?> key) {
		global.remove(key);
		for (StatisticsMap player : byPlayer.values()) {
			player.remove(key);
		}
		for (StatisticsMap team : byTeam.values()) {
			team.remove(key);
		}
	}

	public void clearForPlayers(StatisticKey<?> key) {
		for (StatisticsMap player : byPlayer.values()) {
			player.remove(key);
		}
	}

	public void clearForTeams(StatisticKey<?> key) {
		for (StatisticsMap team : byTeam.values()) {
			team.remove(key);
		}
	}

	public void copyFrom(GameStatistics fromStatistics, Collection<StatisticKey<?>> keys) {
		global.copyFrom(fromStatistics.global, keys);
		fromStatistics.byPlayer.forEach((player, fromPlayerStatistics) ->
				forPlayer(player).copyFrom(fromPlayerStatistics, keys)
		);
		fromStatistics.byTeam.forEach((team, fromTeamStatistics) ->
				forTeam(team).copyFrom(fromTeamStatistics, keys)
		);
	}

	public void copyFrom(GameStatistics fromStatistics) {
		global.copyFrom(fromStatistics.global);
		fromStatistics.byPlayer.forEach((player, fromPlayerStatistics) ->
				forPlayer(player).copyFrom(fromPlayerStatistics)
		);
		fromStatistics.byTeam.forEach((team, fromTeamStatistics) ->
				forTeam(team).copyFrom(fromTeamStatistics)
		);
	}

	private record PlayerEntry(
			PlayerKey profile,
			StatisticsMap statistics
	) {
		public static final Codec<PlayerEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
				PlayerKey.FULL_CODEC.fieldOf("profile").forGetter(PlayerEntry::profile),
				StatisticsMap.CODEC.fieldOf("statistics").forGetter(PlayerEntry::statistics)
		).apply(i, PlayerEntry::new));
	}

	private record TeamEntry(
			GameTeamKey id,
			StatisticsMap statistics
	) {
		public static final Codec<TeamEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
				GameTeamKey.CODEC.fieldOf("id").forGetter(TeamEntry::id),
				StatisticsMap.CODEC.fieldOf("statistics").forGetter(TeamEntry::statistics)
		).apply(i, TeamEntry::new));
	}
}
