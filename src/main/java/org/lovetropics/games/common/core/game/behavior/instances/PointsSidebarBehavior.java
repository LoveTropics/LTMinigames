package org.lovetropics.games.common.core.game.behavior.instances;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.state.statistics.LeaderboardConfig;
import org.lovetropics.games.common.core.game.state.statistics.PlayerKey;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;
import org.lovetropics.games.common.core.game.state.team.GameTeamKey;
import org.lovetropics.games.common.core.game.util.GameSidebar;
import org.lovetropics.games.common.core.game.util.GameWidgets;
import org.lovetropics.games.common.core.game.util.TemplatedText;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record PointsSidebarBehavior(
		Component title,
		List<TemplatedText> header,
		LeaderboardConfig<?> leaderboard
) implements IGameBehavior {
	public static final MapCodec<PointsSidebarBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ComponentSerialization.CODEC.fieldOf("title").forGetter(PointsSidebarBehavior::title),
			TemplatedText.CODEC.listOf().fieldOf("header").forGetter(PointsSidebarBehavior::header),
			LeaderboardConfig.MAP_CODEC.forGetter(PointsSidebarBehavior::leaderboard)
	).apply(i, PointsSidebarBehavior::new));

	private static final int REFRESH_INTERVAL = SharedConstants.TICKS_PER_SECOND / 2;

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		GameSidebar sidebar = GameWidgets.getOrRegister(game, events).openGlobalSidebar(title);
		events.listen(GamePhaseEvents.TICK, () -> {
			if (game.ticks() % REFRESH_INTERVAL == 0) {
				sidebar.set(renderSidebar(game));
			}
		});
	}

	private Component[] renderSidebar(IGamePhase game) {
		int totalCount = 0;
		if (leaderboard.statistic().type() == Integer.class) {
			//noinspection unchecked
			StatisticKey<Integer> intStat = (StatisticKey<Integer>) leaderboard.statistic();
			for (PlayerKey player : game.statistics().getPlayers()) {
				totalCount += game.statistics().readForPlayer(player).getInt(intStat);
			}
			for (GameTeamKey team : game.statistics().getTeams()) {
				totalCount += game.statistics().forTeam(team).getInt(intStat);
			}
		}

		List<Component> sidebar = new ArrayList<>(10);
		for (TemplatedText line : header) {
			sidebar.add(line.apply(Map.of("total", Component.literal(String.valueOf(totalCount)))));
		}
		leaderboard.buildDisplay(game, sidebar::add);
		return sidebar.toArray(new Component[0]);
	}
}
