package com.lovetropics.minigames.common.core.game.behavior.instances.statistics;

import com.lovetropics.minigames.common.core.game.GameWinner;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameLogicEvents;
import com.lovetropics.minigames.common.core.game.state.statistics.PlayerKey;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.state.team.GameTeam;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

public record SetWinnerStatisticBehavior() implements IGameBehavior {
	public static final MapCodec<SetWinnerStatisticBehavior> CODEC = MapCodec.unit(SetWinnerStatisticBehavior::new);

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GameLogicEvents.GAME_OVER, winner -> {
			switch (winner) {
				case GameWinner.Player(ServerPlayer player) ->
						game.statistics().global().set(StatisticKey.WINNING_PLAYER, PlayerKey.from(player));
				case GameWinner.OfflinePlayer(PlayerKey playerKey, Component ignored) ->
						game.statistics().global().set(StatisticKey.WINNING_PLAYER, playerKey);
				case GameWinner.Team(GameTeam team) ->
						game.statistics().global().set(StatisticKey.WINNING_TEAM, team.key());
				case GameWinner.Nobody ignored -> {
				}
			}
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.SET_WINNER_STATISTIC;
	}
}
