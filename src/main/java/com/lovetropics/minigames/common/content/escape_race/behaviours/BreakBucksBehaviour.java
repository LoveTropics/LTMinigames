package com.lovetropics.minigames.common.content.escape_race.behaviours;

import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import com.lovetropics.minigames.common.content.escape_race.client.EscapeRaceClientBucksState;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.DdrLevelDifficulty;
import com.lovetropics.minigames.common.content.escape_race.event.EscapeRaceEvents;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class BreakBucksBehaviour implements IGameBehavior {
	public static final MapCodec<BreakBucksBehaviour> CODEC = MapCodec.unit(BreakBucksBehaviour::new);

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		TeamState teams = game.instanceState().getOrThrow(TeamState.KEY);
		GameClientState.applyGlobally(game, events, SharedConstants.TICKS_PER_SECOND, EscapeRace.BREAK_BUCK_STATE.get(), player -> {
			GameTeamKey teamForPlayer = teams.getTeamForPlayer(player);
			if (teamForPlayer == null) {
				return null;
			}
			int breakBucks = game.statistics().forTeam(teamForPlayer).getInt(StatisticKey.BREAK_BUCKS);
			return new EscapeRaceClientBucksState(breakBucks);
		});
		events.listen(EscapeRaceEvents.DDR_LEVEL_COMPLETED, (player, level, score, bestStreak) -> {
			GameTeamKey teamForPlayer = teams.getTeamForPlayer(player);
			if(teamForPlayer != null){
				//Rough break bucks conversion....
				DdrLevelDifficulty difficulty = level.value().difficulty();
				int breakBucks = Math.round(difficulty.getScoreMultiplier() * score);
				breakBucks += (int) ((bestStreak * 2) * difficulty.getScoreMultiplier());
				addBreakBucks(game, teams, teamForPlayer, breakBucks);
			}
		});

		events.listen(GamePhaseEvents.REGISTER_COMMANDS, (commands, buildContext) -> {
			commands.register(Commands.literal("breakbucks")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.then(Commands.literal("add")
							.then(Commands.argument("amount", IntegerArgumentType.integer())
									.executes(context -> {
										ServerPlayer player = context.getSource().getPlayerOrException();
										GameTeamKey teamForPlayer = teams.getTeamForPlayer(player);
										if (teamForPlayer != null) {
											int amount = IntegerArgumentType.getInteger(context, "amount");
											addBreakBucks(game, teams, teamForPlayer, amount);
										}
										return 1;
									})
							)
					)
			);
		});
	}

	private void addBreakBucks(IGamePhase game, TeamState teams, GameTeamKey team, int amount) {
		game.statistics().forTeam(team).incrementInt(StatisticKey.BREAK_BUCKS, amount);
		teams.getPlayersForTeam(game, team).sendMessage(
				Component.translatable("ltminigames.minigame.escape_race.ddr.score.added", amount).withStyle(ChatFormatting.GOLD)
		);
	}
}
