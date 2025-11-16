package com.lovetropics.minigames.common.core.game.behavior.action;

import com.google.common.collect.Lists;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.player.PlayerIterable;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Collection;
import java.util.List;

public record ActionSubjects<T>(ActionSubjectType<T> type, List<T> instances) {
	public static final ActionSubjects<Void> EMPTY = new ActionSubjects<>(ActionSubjectType.VOID, List.of());

	public static ActionSubjects<ServerPlayer> ofPlayers(List<ServerPlayer> players) {
		return new ActionSubjects<>(ActionSubjectType.PLAYER, players);
	}

	public static ActionSubjects<ServerPlayer> ofPlayers(PlayerIterable players) {
		return ofPlayers(Lists.newArrayList(players));
	}

	public static ActionSubjects<ServerPlayer> ofPlayer(ServerPlayer player) {
		return ofPlayers(List.of(player));
	}

	public static ActionSubjects<Entity> ofEntities(Collection<? extends Entity> entities) {
		return new ActionSubjects<>(ActionSubjectType.ENTITY, List.copyOf(entities));
	}

	public static ActionSubjects<Entity> ofEntity(Entity entity) {
		return new ActionSubjects<>(ActionSubjectType.ENTITY, List.of(entity));
	}

	public static ActionSubjects<GameTeamKey> ofTeam(GameTeamKey team) {
		return new ActionSubjects<>(ActionSubjectType.TEAM, List.of(team));
	}

	public static ActionSubjects<GameTeamKey> ofTeams(List<GameTeamKey> teams) {
		return new ActionSubjects<>(ActionSubjectType.TEAM, teams);
	}

	public static ActionSubjects<Plot> ofPlot(Plot plot) {
		return new ActionSubjects<>(ActionSubjectType.PLOT, List.of(plot));
	}

	public List<ServerPlayer> asPlayers(IGamePhase game) {
		return type.asPlayers(game, instances);
	}

	public List<GameTeamKey> asTeams(IGamePhase game) {
		return asType(game, ActionSubjectType.TEAM);
	}

	public List<Plot> asPlots(IGamePhase game) {
		return asType(game, ActionSubjectType.PLOT);
	}

	public List<Entity> asEntities(IGamePhase game) {
		return asType(game, ActionSubjectType.ENTITY);
	}

	@SuppressWarnings("unchecked")
	public <U> List<U> asType(IGamePhase game, ActionSubjectType<U> type) {
		if (type.equals(this.type)) {
			return (List<U>) instances;
		}
		return type.fromPlayers(game, asPlayers(game));
	}

	@SuppressWarnings("unchecked")
	public <U> ActionSubjects<U> coerceInto(IGamePhase game, ActionSubjectType<U> type) {
		if (type.equals(this.type)) {
			return (ActionSubjects<U>) this;
		}
		return new ActionSubjects<>(type, asType(game, type));
	}
}
