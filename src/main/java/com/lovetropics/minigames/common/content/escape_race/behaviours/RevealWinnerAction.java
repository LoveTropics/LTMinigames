package com.lovetropics.minigames.common.content.escape_race.behaviours;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.state.statistics.Placement;
import com.lovetropics.minigames.common.core.game.state.statistics.PlacementOrder;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;

import java.util.List;

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
