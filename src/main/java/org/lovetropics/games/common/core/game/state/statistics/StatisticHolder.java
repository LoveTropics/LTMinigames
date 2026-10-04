package org.lovetropics.games.common.core.game.state.statistics;

import net.minecraft.network.chat.Component;
import org.lovetropics.games.common.core.game.IGamePhase;

public interface StatisticHolder {
	Component getName(IGamePhase game);

	StatisticsMap getOwnStatistics(GameStatistics statistics);
}
