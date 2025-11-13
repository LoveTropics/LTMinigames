package com.lovetropics.minigames.common.core.game.behavior.instances.statistics;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.state.statistics.GameStatistics;
import com.lovetropics.minigames.common.core.game.state.statistics.PlayerKey;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.state.team.GameTeam;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;

public record SumStatisticBehavior(StatisticKey<Integer> statistic, boolean forTeam) implements IGameBehavior {
	public static final MapCodec<SumStatisticBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			StatisticKey.INT_CODEC.fieldOf("statistic").forGetter(SumStatisticBehavior::statistic),
			Codec.BOOL.optionalFieldOf("for_team", false).forGetter(SumStatisticBehavior::forTeam)
	).apply(i, SumStatisticBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		GameStatistics statistics = game.statistics();
		events.listen(GamePhaseEvents.FINISH, () -> {
			int total = 0;
			for (PlayerKey player : statistics.getPlayers()) {
				total += statistics.forPlayer(player).getInt(statistic);
			}
			for (GameTeamKey team : statistics.getTeams()) {
				total += statistics.forTeam(team).getInt(statistic);
			}
			statistics.global().set(statistic, total);
		});

		if (forTeam) {
			TeamState teams = game.instanceState().getOrThrow(TeamState.KEY);
			events.listen(GamePhaseEvents.TICK, () -> {
				for (GameTeam team : teams) {
					int total = 0;
					for (ServerPlayer player : teams.getPlayersForTeam(game, team.key())) {
						total += statistics.forPlayer(player).getInt(statistic);
					}
					statistics.forTeam(team.key()).set(statistic, total);
				}
			});
		}
	}
}
