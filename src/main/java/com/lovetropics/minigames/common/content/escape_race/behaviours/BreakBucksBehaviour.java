package com.lovetropics.minigames.common.content.escape_race.behaviours;

import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import com.lovetropics.minigames.common.content.escape_race.client.EscapeRaceClientBucksState;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.mojang.serialization.MapCodec;
import net.minecraft.SharedConstants;

public class BreakBucksBehaviour implements IGameBehavior {
	public static final MapCodec<BreakBucksBehaviour> CODEC = MapCodec.unit(BreakBucksBehaviour::new);

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		GameClientState.applyGlobally(game, events, SharedConstants.TICKS_PER_SECOND, EscapeRace.BREAK_BUCK_STATE.get(), player -> {
			TeamState teams = game.instanceState().getOrNull(TeamState.KEY);
			GameTeamKey teamForPlayer = teams.getTeamForPlayer(player);
			int anInt = game.statistics().forTeam(teamForPlayer)
					.getInt(StatisticKey.BREAK_BUCKS);
			return new EscapeRaceClientBucksState(anInt);
		});
	}
}
