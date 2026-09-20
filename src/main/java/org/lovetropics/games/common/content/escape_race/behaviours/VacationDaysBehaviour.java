package org.lovetropics.games.common.content.escape_race.behaviours;

import org.lovetropics.games.common.content.escape_race.EscapeRaceTexts;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;
import org.lovetropics.games.common.core.game.state.team.GameTeam;
import org.lovetropics.games.common.core.game.state.team.TeamState;
import org.lovetropics.games.common.core.game.util.GameSidebar;
import org.lovetropics.games.common.core.game.util.GameWidgets;
import com.mojang.serialization.MapCodec;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class VacationDaysBehaviour implements IGameBehavior {
	public static final MapCodec<VacationDaysBehaviour> CODEC = MapCodec.unit(VacationDaysBehaviour::new);

	private static final int SIDEBAR_INTERVAL = SharedConstants.TICKS_PER_SECOND / 2;

	private IGamePhase game;
	private TeamState teams;

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		this.game = game;
		teams = game.instanceState().getOrThrow(TeamState.KEY);

		GameSidebar sidebar = GameWidgets.getOrRegister(game, events).openGlobalSidebar(EscapeRaceTexts.SIDEBAR_VACATION_DAYS);

		events.listen(GamePhaseEvents.TICK, () -> {
			if (game.ticks() % SIDEBAR_INTERVAL == 0) {
				sidebar.set(renderSidebar(teams));
			}
		});
	}

	private Component[] renderSidebar(TeamState teams) {
		List<Component> sidebar = new ArrayList<>(10);

		if (teams.size() != 2) {
			// :(
			return new Component[0];
		}

		Iterator<GameTeam> iterator = teams.iterator();
		GameTeam firstTeam = iterator.next();
		GameTeam secondTeam = iterator.next();

		sidebar.add(EscapeRaceTexts.SIDEBAR_HEADER.apply(
				Component.literal(String.valueOf(game.statistics().forTeam(firstTeam.key()).getInt(StatisticKey.VACATION_DAYS))),
				firstTeam.config().styledName(),
				Component.literal(String.valueOf(game.statistics().forTeam(secondTeam.key()).getInt(StatisticKey.VACATION_DAYS))),
				secondTeam.config().styledName()
		));

		return sidebar.toArray(new Component[0]);
	}
}
