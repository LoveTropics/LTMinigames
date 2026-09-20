package org.lovetropics.games.common.core.game.behavior.instances.statistics;

import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.state.statistics.GameStatistics;
import org.lovetropics.games.common.core.game.state.statistics.PlayerKey;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;
import org.lovetropics.games.common.core.game.state.statistics.StatisticsMap;
import org.lovetropics.games.common.core.game.state.team.GameTeamKey;
import org.lovetropics.games.common.core.game.state.team.TeamState;
import org.lovetropics.games.common.util.Util;
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

			ServerPlayer killerPlayer = Util.getKillerPlayer(player, damageSource);
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
