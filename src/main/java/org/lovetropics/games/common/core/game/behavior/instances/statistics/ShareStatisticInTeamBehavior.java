package org.lovetropics.games.common.core.game.behavior.instances.statistics;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.state.statistics.GameStatistics;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;
import org.lovetropics.games.common.core.game.state.team.GameTeamKey;
import org.lovetropics.games.common.core.game.state.team.TeamState;

public record ShareStatisticInTeamBehavior(StatisticKey<?> statistic) implements IGameBehavior {
	public static final MapCodec<ShareStatisticInTeamBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			StatisticKey.CODEC.fieldOf("statistic").forGetter(ShareStatisticInTeamBehavior::statistic)
	).apply(i, ShareStatisticInTeamBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		GameStatistics statistics = game.statistics();
		TeamState teams = game.instanceState().getOrThrow(TeamState.KEY);
		events.listen(GamePhaseEvents.FINISH, () -> copyForTeam(game, teams, statistics, statistic));
	}

	private static <T> void copyForTeam(IGamePhase game, TeamState teams, GameStatistics statistics, StatisticKey<T> statistic) {
		for (GameTeamKey team : statistics.getTeams()) {
			T value = statistics.forTeam(team).get(statistic);
			for (ServerPlayer player : teams.getPlayersForTeam(game, team)) {
				statistics.forPlayer(player).set(statistic, value);
			}
		}
	}
}
