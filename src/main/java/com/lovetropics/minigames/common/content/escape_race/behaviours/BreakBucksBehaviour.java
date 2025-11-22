package com.lovetropics.minigames.common.content.escape_race.behaviours;

import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import com.lovetropics.minigames.common.content.escape_race.EscapeRaceParticles;
import com.lovetropics.minigames.common.content.escape_race.EscapeRaceTexts;
import com.lovetropics.minigames.common.content.escape_race.client.EscapeRaceClientBucksState;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.DdrLevelDifficulty;
import com.lovetropics.minigames.common.content.escape_race.event.EscapeRaceEvents;
import com.lovetropics.minigames.common.content.escape_race.vending_machine.VendingMachineEvents;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticsMap;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;

public record BreakBucksBehaviour(
		float particleSpreadX,
		float particleSpreadY,
		float particleSpreadZ,
		int buckMultiplier) implements IGameBehavior {
	public static final MapCodec<BreakBucksBehaviour> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.FLOAT.optionalFieldOf("particle_spread_x", 1.5f).forGetter(BreakBucksBehaviour::particleSpreadX),
			Codec.FLOAT.optionalFieldOf("particle_spread_y", 1.5f).forGetter(BreakBucksBehaviour::particleSpreadY),
			Codec.FLOAT.optionalFieldOf("particle_spread_z", 1.5f).forGetter(BreakBucksBehaviour::particleSpreadZ),
			Codec.INT.optionalFieldOf("buck_divider", 1).forGetter(BreakBucksBehaviour::buckMultiplier)
	).apply(instance, BreakBucksBehaviour::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		TeamState teams = game.instanceState().getOrThrow(TeamState.KEY);
		Object2IntMap<GameTeamKey> lastTeamPoints = new Object2IntArrayMap<>();

		GameClientState.applyGlobally(game, events, SharedConstants.TICKS_PER_SECOND, EscapeRace.BREAK_BUCK_STATE.get(), player -> {
			GameTeamKey teamForPlayer = teams.getTeamForPlayer(player);
			if (teamForPlayer == null) {
				return null;
			}
			int newBucks = game.statistics().forTeam(teamForPlayer).getInt(StatisticKey.BREAK_BUCKS);
			int oldBucks = lastTeamPoints.put(teamForPlayer, newBucks);
			if (oldBucks != newBucks) {
				PlayerSet playersForTeam = teams.getPlayersForTeam(game, teamForPlayer);
				for (ServerPlayer serverPlayer : playersForTeam) {
					serverPlayer.level().sendParticles(EscapeRaceParticles.BREAK_BUCK_PARTICLE.get(),
							serverPlayer.getX(), serverPlayer.getY() + 1.0, serverPlayer.getZ(),
							Math.abs(1 + ((newBucks - oldBucks) / buckMultiplier)), // Look where in a rush here
							particleSpreadX, particleSpreadY, particleSpreadZ,
							0.1f);
				}
			}
			return new EscapeRaceClientBucksState(newBucks);
		});
		events.listen(EscapeRaceEvents.DDR_LEVEL_COMPLETED, (player, level, score, bestStreak) -> {
			GameTeamKey team = teams.getTeamForPlayer(player);
			if (team != null) {
				//Rough break bucks conversion....
				DdrLevelDifficulty difficulty = level.value().difficulty();
				int breakBucks = Math.round(difficulty.getScoreMultiplier() * score);
				breakBucks += (int) ((bestStreak * 2) * difficulty.getScoreMultiplier());
				breakBucks = Math.round(breakBucks / 10f);
				addBreakBucks(game, team, breakBucks);
				Component message = EscapeRaceTexts.DDR_SCORE_ADDED.apply(player.getDisplayName(), breakBucks).withStyle(ChatFormatting.GOLD);
				teams.getPlayersForTeam(game, team).sendMessage(message);
			}
		});

		events.listen(VendingMachineEvents.PURCHASE_ITEM, (player, entity, item) -> {
			GameTeamKey team = teams.getTeamForPlayer(player);
			if (team == null) {
				return TriState.FALSE;
			}
			StatisticsMap teamStatistics = game.statistics().forTeam(team);
			int cost = item.getOrDefault(EscapeRace.VENDING_MACHINE_COST, 0);
			int breakBucks = teamStatistics.getInt(StatisticKey.BREAK_BUCKS);
			if (breakBucks >= cost) {
				Component message = EscapeRaceTexts.SPENT_BREAK_BUCKS.apply(player.getDisplayName(), cost).withStyle(ChatFormatting.GRAY);
				teams.getPlayersForTeam(game, team).sendMessage(message);
				teamStatistics.incrementInt(StatisticKey.BREAK_BUCKS, -cost);
				return TriState.TRUE;
			}
			return TriState.FALSE;
		});

		// TODO: Replace with PointControlCommandsBehavior (though we need to figure out notifications and team targeting!)
		events.listen(GamePhaseEvents.REGISTER_COMMANDS, (commands, buildContext) -> {
			commands.register(Commands.literal("breakbucks")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.then(Commands.literal("add")
							.then(Commands.argument("amount", IntegerArgumentType.integer())
									.executes(context -> {
										ServerPlayer player = context.getSource().getPlayerOrException();
										int amount = IntegerArgumentType.getInteger(context, "amount");
										giveBreakBucks(game, teams, player, amount);
										return 1;
									})
							)
					)
			);
		});
	}

	private void giveBreakBucks(IGamePhase game, TeamState teams, ServerPlayer player, int amount) {
		GameTeamKey team = teams.getTeamForPlayer(player);
		if (team != null) {
			addBreakBucks(game, team, amount);
			Component message = EscapeRaceTexts.GIVEN_BREAK_BUCKS.apply(amount).withStyle(ChatFormatting.GOLD);
			teams.getPlayersForTeam(game, team).sendMessage(message);
		}
	}

	public static void addBreakBucks(IGamePhase game, GameTeamKey team, int amount) {
		game.statistics().forTeam(team).incrementInt(StatisticKey.BREAK_BUCKS, amount);
	}
}
