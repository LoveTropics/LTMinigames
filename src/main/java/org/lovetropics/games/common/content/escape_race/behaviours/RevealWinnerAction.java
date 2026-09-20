package org.lovetropics.games.common.content.escape_race.behaviours;

import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.core.game.state.statistics.Placement;
import org.lovetropics.games.common.core.game.state.statistics.PlacementOrder;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;
import org.lovetropics.games.common.core.game.state.team.GameTeamKey;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;

public record RevealWinnerAction() implements IGameBehavior {
	public static final MapCodec<RevealWinnerAction> CODEC = MapCodec.unit(RevealWinnerAction::new);

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GameActionEvents.APPLY, ((context, targets) -> {
			Placement.Score<GameTeamKey, Integer> teamPlacement = Placement.fromTeamScore(PlacementOrder.MAX, game, StatisticKey.VACATION_DAYS);
			GameTeamKey winner = teamPlacement.getWinner();
			game.allPlayers().showTitle(
					winner.getName(game),
					Component.literal("is the winner!"),
					20,
					80,
					20
			);
			return true;
		}));
	}

}
