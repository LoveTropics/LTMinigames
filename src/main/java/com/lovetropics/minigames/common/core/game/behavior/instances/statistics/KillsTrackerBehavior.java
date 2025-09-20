package com.lovetropics.minigames.common.core.game.behavior.instances.statistics;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.state.statistics.GameStatistics;
import com.lovetropics.minigames.common.core.game.state.statistics.PlayerKey;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticsMap;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.lovetropics.minigames.common.util.Util;
import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;

public final class KillsTrackerBehavior implements IGameBehavior {
	public static final MapCodec<KillsTrackerBehavior> CODEC = MapCodec.unit(KillsTrackerBehavior::new);

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		TeamState teams = game.instanceState().getOrNull(TeamState.KEY);
		events.listen(GamePlayerEvents.DEATH, (player, damageSource) -> {
			GameStatistics statistics = game.statistics();
			StatisticsMap playerStatistics = statistics.forPlayer(player);

			final ServerPlayer killerPlayer = Util.getKillerPlayer(player, damageSource);
			if (killerPlayer != null) {
				if (killerPlayer != player) {
					statistics.forPlayer(killerPlayer).incrementInt(StatisticKey.KILLS, 1);

					GameTeamKey team = teams != null ? teams.getTeamForPlayer(killerPlayer) : null;
					if (team != null) {
						statistics.forTeam(team).incrementInt(StatisticKey.KILLS, 1);
					}
				}

				playerStatistics.set(StatisticKey.KILLED_BY, PlayerKey.from(killerPlayer));
			}

			return TriState.DEFAULT;
		});
	}
}
