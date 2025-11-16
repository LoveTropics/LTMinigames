package com.lovetropics.minigames.common.content.biodiversity_blitz.plot;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.state.GameStateKey;
import com.lovetropics.minigames.common.core.game.state.IGameState;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class PlotsState implements Iterable<Plot>, IGameState {
	public static final GameStateKey<PlotsState> KEY = GameStateKey.create("Biodiversity Blitz Plots");

	private final TeamState teams;
	private final List<Plot> plots = new ArrayList<>();
	private final Map<GameTeamKey, Plot> plotsByTeam = new Object2ObjectOpenHashMap<>();

	public PlotsState(TeamState teams) {
		this.teams = teams;
	}

	@Nullable
	public Plot getPlotAt(BlockPos pos) {
		for (Plot plot : plots) {
			if (plot.walls.containsBlock(pos)) {
				return plot;
			}
		}
		return null;
	}

	public void addTeamPlot(GameTeamKey team, Plot plot) {
		plotsByTeam.put(team, plot);
		plots.add(plot);
	}

	@Nullable
	public Plot getPlotFor(GameTeamKey team) {
		return plotsByTeam.get(team);
	}

	@Nullable
	public Plot getPlotFor(Entity entity) {
		if (entity instanceof final Player player) {
			GameTeamKey team = teams.getTeamForPlayer(player);
			return team != null ? getPlotFor(team) : null;
		}
		return null;
	}

	public List<ServerPlayer> getPlayersForPlot(IGamePhase game, Plot plot) {
		List<ServerPlayer> players = new ArrayList<>();
		for (ServerPlayer participant : game.participants()) {
			Plot playerPlot = getPlotFor(participant);
			if (playerPlot == plot) {
				players.add(participant);
			}
		}
		return players;
	}

	@Override
	public Iterator<Plot> iterator() {
		return plots.iterator();
	}

	public Stream<Plot> stream() {
		return plots.stream();
	}

	@Nullable
	public Plot getRandomPlot(RandomSource random) {
		if (plots.isEmpty()) {
			return null;
		}
		List<Plot> plots = List.copyOf(this.plots);
		return Util.getRandom(plots, random);
	}
}
