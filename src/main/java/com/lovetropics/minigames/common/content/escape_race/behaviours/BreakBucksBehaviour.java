package com.lovetropics.minigames.common.content.escape_race.behaviours;

import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import com.lovetropics.minigames.common.content.escape_race.client.EscapeRaceClientBucksState;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.DdrLevelDifficulty;
import com.lovetropics.minigames.common.content.escape_race.event.EscapeRaceEvents;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;

public class BreakBucksBehaviour implements IGameBehavior {
	public static final MapCodec<BreakBucksBehaviour> CODEC = MapCodec.unit(BreakBucksBehaviour::new);

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		TeamState teams = game.instanceState().getOrNull(TeamState.KEY);
		GameClientState.applyGlobally(game, events, SharedConstants.TICKS_PER_SECOND, EscapeRace.BREAK_BUCK_STATE.get(), player -> {
			GameTeamKey teamForPlayer = teams.getTeamForPlayer(player);
			int anInt = game.statistics().forTeam(teamForPlayer)
					.getInt(StatisticKey.BREAK_BUCKS);
			return new EscapeRaceClientBucksState(anInt);
		});
		events.listen(EscapeRaceEvents.DDR_LEVEL_COMPLETED, (player, level, score, bestStreak) -> {
			GameTeamKey teamForPlayer = teams.getTeamForPlayer(player);
			if(teamForPlayer != null){
				//Rough break bucks conversion....
				DdrLevelDifficulty difficulty = level.value().difficulty();
				int breakBucks = Math.round(difficulty.getScoreMultiplier() * score);
				breakBucks += (int) ((bestStreak * 2) * difficulty.getScoreMultiplier());
				game.statistics().forTeam(teamForPlayer).incrementInt(StatisticKey.BREAK_BUCKS, breakBucks);
				teams.getPlayersForTeam(game, teamForPlayer)
						.sendMessage(Component.translatable("ltminigames.minigame.escape_race.ddr.score.added", breakBucks)
								.withStyle(ChatFormatting.GOLD));
			}
		});
	}
}
