package com.lovetropics.minigames.common.core.game.state.statistics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lovetropics.minigames.common.core.game.state.GameStateKey;
import com.lovetropics.minigames.common.core.game.state.IGameState;
import com.lovetropics.minigames.common.core.game.state.team.GameTeam;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GameStatistics implements IGameState {
	public static final GameStateKey.Defaulted<GameStatistics> KEY = GameStateKey.create("Game Statistics", GameStatistics::new);

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

	public JsonElement serialize() {
		JsonObject root = new JsonObject();
		root.add("global", global.serialize());

		JsonArray playersArray = new JsonArray();
		for (Map.Entry<PlayerKey, StatisticsMap> entry : byPlayer.entrySet()) {
			PlayerKey player = entry.getKey();

			JsonObject playerRoot = new JsonObject();
			playerRoot.add("profile", player.serializeProfile());
			playerRoot.add("statistics", entry.getValue().serialize());

			playersArray.add(playerRoot);
		}

		root.add("players", playersArray);

		JsonArray teamsArray = new JsonArray();
		for (Map.Entry<GameTeamKey, StatisticsMap> entry : byTeam.entrySet()) {
			GameTeamKey team = entry.getKey();

			JsonObject teamRoot = new JsonObject();
			teamRoot.addProperty("id", team.id());
			teamRoot.add("statistics", entry.getValue().serialize());

			teamsArray.add(teamRoot);
		}

		return root;
	}

	public void copyFrom(GameStatistics fromStatistics, List<StatisticKey<?>> keys) {
		global.copyFrom(fromStatistics.global, keys);
		fromStatistics.byPlayer.forEach((player, fromPlayerStatistics) ->
				forPlayer(player).copyFrom(fromPlayerStatistics, keys)
		);
		fromStatistics.byTeam.forEach((team, fromTeamStatistics) ->
				forTeam(team).copyFrom(fromTeamStatistics, keys)
		);
	}
}
