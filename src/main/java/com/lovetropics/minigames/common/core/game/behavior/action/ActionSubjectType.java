package com.lovetropics.minigames.common.core.game.behavior.action;

import com.google.common.collect.Lists;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.PlotsState;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

public interface ActionSubjectType<T> {
	ActionSubjectType<Void> VOID = new ActionSubjectType<>() {
		@Override
		public List<ServerPlayer> asPlayers(IGamePhase game, List<Void> instances) {
			return List.of();
		}

		@Override
		public List<Void> fromPlayers(IGamePhase game, List<ServerPlayer> players) {
			return List.of();
		}
	};

	ActionSubjectType<ServerPlayer> PLAYER = new ActionSubjectType<>() {
		@Override
		public List<ServerPlayer> asPlayers(IGamePhase game, List<ServerPlayer> players) {
			return players;
		}

		@Override
		public List<ServerPlayer> fromPlayers(IGamePhase game, List<ServerPlayer> players) {
			return players;
		}
	};
	ActionSubjectType<Entity> ENTITY = new ActionSubjectType<>() {
		@Override
		public List<ServerPlayer> asPlayers(IGamePhase game, List<Entity> entities) {
			List<ServerPlayer> players = new ArrayList<>(entities.size());
			for (Entity entity : entities) {
				if (entity instanceof ServerPlayer player) {
					players.add(player);
				}
			}
			return players;
		}

		@Override
		public List<Entity> fromPlayers(IGamePhase game, List<ServerPlayer> players) {
			return Lists.transform(players, p -> p);
		}
	};
	ActionSubjectType<GameTeamKey> TEAM = new ActionSubjectType<>() {
		@Override
		public List<ServerPlayer> asPlayers(IGamePhase game, List<GameTeamKey> teams) {
			TeamState teamState = game.instanceState().getOrNull(TeamState.KEY);
			if (teamState == null) {
				return List.of();
			}
			List<ServerPlayer> players = new ArrayList<>();
			for (GameTeamKey team : teams) {
				teamState.getPlayersForTeam(game, team).forEach(players::add);
			}
			return players;
		}

		@Override
		public List<GameTeamKey> fromPlayers(IGamePhase game, List<ServerPlayer> players) {
			TeamState teamState = game.instanceState().getOrNull(TeamState.KEY);
			if (teamState == null) {
				return List.of();
			}
			List<GameTeamKey> teams = new ArrayList<>(2);
			for (ServerPlayer player : players) {
				GameTeamKey team = teamState.getTeamForPlayer(player);
				if (team != null && !teams.contains(team)) {
					teams.add(team);
				}
			}
			return teams;
		}
	};
	ActionSubjectType<Plot> PLOT = new ActionSubjectType<>() {
		@Override
		public List<ServerPlayer> asPlayers(IGamePhase game, List<Plot> plots) {
			PlotsState plotsState = game.state().getOrNull(PlotsState.KEY);
			if (plotsState == null) {
				return List.of();
			}
			List<ServerPlayer> players = new ArrayList<>();
			for (Plot plot : plots) {
				players.addAll(plotsState.getPlayersForPlot(game, plot));
			}
			return players;
		}

		@Override
		public List<Plot> fromPlayers(IGamePhase game, List<ServerPlayer> players) {
			PlotsState plotsState = game.state().getOrNull(PlotsState.KEY);
			if (plotsState == null) {
				return List.of();
			}
			List<Plot> plots = new ArrayList<>();
			for (ServerPlayer player : players) {
				Plot plot = plotsState.getPlotFor(player);
				if (plot != null && !plots.contains(plot)) {
					plots.add(plot);
				}
			}
			return plots;
		}
	};

	// For now, coercing through players works well enough
	List<ServerPlayer> asPlayers(IGamePhase game, List<T> instances);

	List<T> fromPlayers(IGamePhase game, List<ServerPlayer> players);
}
