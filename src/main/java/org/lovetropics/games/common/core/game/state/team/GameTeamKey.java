package org.lovetropics.games.common.core.game.state.team;

import org.lovetropics.games.common.content.MinigameTexts;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.state.statistics.GameStatistics;
import org.lovetropics.games.common.core.game.state.statistics.StatisticHolder;
import org.lovetropics.games.common.core.game.state.statistics.StatisticsMap;
import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;

public record GameTeamKey(String id) implements StatisticHolder {
	public static final Codec<GameTeamKey> CODEC = Codec.STRING.xmap(GameTeamKey::new, GameTeamKey::id);

	@Override
	public String toString() {
		return id;
	}

	@Override
	public Component getName(IGamePhase game) {
		TeamState teams = game.instanceState().getOrNull(TeamState.KEY);
		GameTeam team = teams != null ? teams.getTeamByKey(this) : null;
		return team != null ? team.styledName() : MinigameTexts.UNKNOWN;
	}

	@Override
	public StatisticsMap getOwnStatistics(GameStatistics statistics) {
		return statistics.forTeam(this);
	}
}
