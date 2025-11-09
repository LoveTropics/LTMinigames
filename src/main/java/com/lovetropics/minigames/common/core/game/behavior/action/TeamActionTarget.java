package com.lovetropics.minigames.common.core.game.behavior.action;

import com.google.common.collect.Lists;
import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GameEventListeners;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.state.team.GameTeam;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import org.apache.commons.lang3.function.ToBooleanBiFunction;

import java.util.List;

public record TeamActionTarget(Either<BuiltinType, GameTeamKey> team) implements ActionTarget<GameTeam> {
	public static final TeamActionTarget SOURCE = new TeamActionTarget(Either.left(BuiltinType.SOURCE));
	public static final TeamActionTarget ALL = new TeamActionTarget(Either.left(BuiltinType.ALL));
	public static final Codec<TeamActionTarget> CODEC = Codec.either(BuiltinType.CODEC, GameTeamKey.CODEC)
			.xmap(TeamActionTarget::new, TeamActionTarget::team);

	@Override
	public List<GameTeam> resolve(IGamePhase phase, Iterable<GameTeam> sources) {
		TeamState teams = phase.instanceState().getOrNull(TeamState.KEY);
		if (teams == null) {
			return List.of();
		}
		return team.map(
				builtinType -> builtinType.resolve(sources, teams),
				teamKey -> {
					GameTeam team = teams.getTeamByKey(teamKey);
					return team != null ? List.of(team) : List.of();
				}
		);
	}

	@Override
	public boolean apply(IGamePhase game, GameEventListeners listeners, GameActionContext actionContext, Iterable<GameTeam> sources) {
		boolean result = false;
		for (GameTeam team : resolve(game, sources)) {
			result |= listeners.invoker(GameActionEvents.APPLY_TO_TEAM).apply(actionContext, team);
			TeamState teams = game.instanceState().getOrNull(TeamState.KEY);
			PlayerSet players = teams != null ? teams.getPlayersForTeam(team.key()) : PlayerSet.EMPTY;
			for (ServerPlayer player : players) {
				result |= listeners.invoker(GameActionEvents.APPLY_TO_PLAYER).apply(actionContext, player);
				result |= listeners.invoker(GameActionEvents.APPLY_TO_ENTITY).apply(actionContext, player);
			}
		}
		return result;
	}

	@Override
	public void listenAndCaptureSource(EventRegistrar listeners, ToBooleanBiFunction<GameActionContext, Iterable<GameTeam>> listener) {
		listeners.listen(GameActionEvents.APPLY_TO_TEAM, (context, team) -> listener.applyAsBoolean(context, List.of(team)));
	}

	@Override
	public Codec<TeamActionTarget> type() {
		return ActionTargetTypes.TEAM.get();
	}

	public enum BuiltinType implements StringRepresentable {
		NONE("none"),
		SOURCE("source"),
		ALL("all"),
		;

		public static final Codec<BuiltinType> CODEC = MoreCodecs.stringVariants(values(), BuiltinType::getSerializedName);

		private final String name;

		BuiltinType(String name) {
			this.name = name;
		}

		public List<GameTeam> resolve(Iterable<GameTeam> sources, TeamState teams) {
			return switch (this) {
				case NONE -> List.of();
				case SOURCE -> Lists.newArrayList(sources);
				case ALL -> Lists.newArrayList(teams);
			};
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}
}
