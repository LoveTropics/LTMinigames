package org.lovetropics.games.common.core.game.behavior.instances.statistics;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.lovetropics.games.common.core.game.GameWinner;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.core.game.behavior.event.GameLogicEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.state.statistics.Placement;
import org.lovetropics.games.common.core.game.state.statistics.PlacementOrder;
import org.lovetropics.games.common.core.game.state.statistics.PlayerKey;
import org.lovetropics.games.common.core.game.state.statistics.StatisticHolderType;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;
import org.lovetropics.games.common.core.game.state.team.GameTeam;
import org.lovetropics.games.common.core.game.state.team.GameTeamKey;
import org.lovetropics.games.common.core.game.state.team.TeamState;

import java.util.Optional;

public record PlaceByStatisticBehavior(StatisticKey<Integer> statistic, PlacementOrder order, Optional<StatisticHolderType> triggerWin) implements IGameBehavior {
	public static final MapCodec<PlaceByStatisticBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			StatisticKey.INT_CODEC.fieldOf("statistic").forGetter(c -> c.statistic),
			PlacementOrder.CODEC.optionalFieldOf("order", PlacementOrder.MAX).forGetter(c -> c.order),
			StatisticHolderType.CODEC.optionalFieldOf("trigger_win").forGetter(c -> c.triggerWin)
	).apply(i, PlaceByStatisticBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		if (triggerWin.isPresent()) {
			events.listen(GameLogicEvents.REQUEST_GAME_OVER, () -> {
				GameWinner winner = runPlacement(game);
				game.invoker(GameLogicEvents.GAME_OVER).onGameOver(winner);
				return true;
			});
		}

		// Just to make sure that the placement statistics are there, even if game_over was never triggered
		events.listen(GamePhaseEvents.FINISH, () -> runPlacement(game));

		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			runPlacement(game);
			return true;
		});
	}

	private GameWinner runPlacement(IGamePhase game) {
		Placement.Score<PlayerKey, Integer> playerPlacement = Placement.fromPlayerScore(order, game, statistic);
		playerPlacement.placeInto(game.statistics(), StatisticKey.PLACEMENT);

		addReversePlacements(game);

		Placement.Score<GameTeamKey, Integer> teamPlacement = Placement.fromTeamScore(order, game, statistic);
		teamPlacement.placeInto(game.statistics(), StatisticKey.PLACEMENT);

		PlayerKey winningPlayerKey = playerPlacement.getWinner();
		GameTeamKey winningTeamKey = teamPlacement.getWinner();

		StatisticHolderType winnerType = triggerWin.orElse(StatisticHolderType.DEFAULT);

		TeamState teams = game.instanceState().getOrNull(TeamState.KEY);
		GameTeam winningTeam = teams != null && winningTeamKey != null ? teams.getTeamByKey(winningTeamKey) : null;
		if (winningTeam != null && winnerType != StatisticHolderType.PLAYER) {
			return new GameWinner.Team(winningTeam);
		} else if (winningPlayerKey != null && winnerType != StatisticHolderType.TEAM) {
			return GameWinner.byPlayerKey(game, winningPlayerKey);
		}
		return new GameWinner.Nobody();
	}

	private void addReversePlacements(IGamePhase game) {
		int participantCount = game.participants().size();
		for (PlayerKey player : game.statistics().getPlayers()) {
			Integer placement = game.statistics().readForPlayer(player).get(StatisticKey.PLACEMENT);
			if (placement == null) {
				continue;
			}
			game.statistics().forPlayer(player).set(StatisticKey.REVERSE_PLACEMENT, Math.max(participantCount - placement, 0));
		}
	}

	@Override
	public MapCodec<? extends IGameBehavior> codec() {
		return CODEC;
	}
}
