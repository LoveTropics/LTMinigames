package com.lovetropics.minigames.common.core.command.game;

import com.lovetropics.minigames.common.core.command.argument.StatisticKeyArgument;
import com.lovetropics.minigames.common.core.command.argument.StatisticValueArgument;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.impl.GamePhaseManager;
import com.lovetropics.minigames.common.core.game.state.statistics.GameStatistics;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticsMap;
import com.lovetropics.minigames.common.core.game.state.team.GameTeam;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.lovetropics.minigames.common.core.game.util.GameTexts;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.arguments.EntityArgument.getPlayer;
import static net.minecraft.commands.arguments.EntityArgument.player;

public class GameStatisticCommand {
	private static final SimpleCommandExceptionType NOT_IN_GAME = new SimpleCommandExceptionType(GameTexts.Commands.NOT_IN_GAME);
	private static final DynamicCommandExceptionType NO_TEAM = new DynamicCommandExceptionType(GameTexts.Commands::noTeam);
	private static final DynamicCommandExceptionType MALFORMED_STATISTICS = new DynamicCommandExceptionType(error -> Component.literal("Could not parse statistics: " + error));

	public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(literal("game").then(literal("stat")
				.requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(literal("import")
						.then(Commands.argument("tag", NbtTagArgument.nbtTag())
								.executes(context -> {
									IGamePhase game = getGameFor(context.getSource());
									GameStatistics statistics = GameStatistics.CODEC.parse(NbtOps.INSTANCE, NbtTagArgument.getNbtTag(context, "tag")).getOrThrow(MALFORMED_STATISTICS::create);
									game.statistics().copyFrom(statistics);
									context.getSource().sendSuccess(() -> Component.literal("Successfully imported statistics"), true);
									return 1;
								})
						)
				)
				.then(StatisticKeyArgument.argument("statistic")
						.then(literal("get")
								.then(literal("player")
										.then(argument("player", player())
												.executes(context -> {
													ServerPlayer player = getPlayer(context, "player");
													StatisticKey<?> statisticKey = StatisticKeyArgument.get(context, "statistic");
													return getStatistic(context.getSource(), s -> s.forPlayer(player), player.getDisplayName(), statisticKey);
												})
										)
								)
								.then(literal("team")
										.then(argument("team", word())
												.suggests(GameStatisticCommand::suggestTeams)
												.executes(context -> {
													GameTeam team = getTeam(context, "team");
													StatisticKey<?> statisticKey = StatisticKeyArgument.get(context, "statistic");
													return getStatistic(context.getSource(), s -> s.forTeam(team), team.config().styledName(), statisticKey);
												})
										)
								)
								.then(literal("global")
										.executes(context -> {
											StatisticKey<?> statisticKey = StatisticKeyArgument.get(context, "statistic");
											return getStatistic(context.getSource(), GameStatistics::global, GameTexts.Commands.GAME_STATISTICS, statisticKey);
										})
								)
						)
						.then(literal("set")
								.then(literal("player")
										.then(argument("player", player())
												.then(StatisticValueArgument.argument("value")
														.executes(context -> {
															ServerPlayer player = getPlayer(context, "player");
															StatisticKey<?> statisticKey = StatisticKeyArgument.get(context, "statistic");
															return setStatistic(context, s -> s.forPlayer(player), player.getDisplayName(), statisticKey);
														})
												)
										)
								)
								.then(literal("team")
										.then(argument("team", word())
												.suggests(GameStatisticCommand::suggestTeams)
												.then(StatisticValueArgument.argument("value")
														.executes(context -> {
															GameTeam team = getTeam(context, "team");
															StatisticKey<?> statisticKey = StatisticKeyArgument.get(context, "statistic");
															return setStatistic(context, s -> s.forTeam(team), team.config().styledName(), statisticKey);
														})
												)
										)
								)
								.then(literal("global")
										.then(StatisticValueArgument.argument("value")
												.executes(context -> {
													StatisticKey<?> statisticKey = StatisticKeyArgument.get(context, "statistic");
													return setStatistic(context, GameStatistics::global, GameTexts.Commands.GAME_STATISTICS, statisticKey);
												})
										)
								)
						)
				)
		));
	}

	private static IGamePhase getGameFor(CommandSourceStack source) throws CommandSyntaxException {
		IGamePhase game = GamePhaseManager.get().getGamePhaseFor(source);
		if (game == null) {
			throw NOT_IN_GAME.create();
		}
		return game;
	}

	private static <T> int getStatistic(CommandSourceStack source, Function<GameStatistics, StatisticsMap> statisticExtractor, Component targetName, StatisticKey<T> statisticKey) throws CommandSyntaxException {
		T value = statisticExtractor.apply(getGameFor(source).statistics()).get(statisticKey);
		return displayStatisticValue(source, targetName, statisticKey, value);
	}

	private static <T> int setStatistic(CommandContext<CommandSourceStack> context, Function<GameStatistics, StatisticsMap> statisticExtractor, Component targetName, StatisticKey<T> statisticKey) throws CommandSyntaxException {
		T value = StatisticValueArgument.get(context, "value", statisticKey);

		StatisticsMap statisticsMap = statisticExtractor.apply(getGameFor(context.getSource()).statistics());
		statisticsMap.set(statisticKey, value);

		return displayStatisticValue(context.getSource(), targetName, statisticKey, value);
	}

	private static <T> int displayStatisticValue(CommandSourceStack source, Component targetName, StatisticKey<T> statisticKey, @Nullable T value) {
		source.sendSuccess(() -> GameTexts.Commands.statisticValue(statisticKey, targetName, value), false);
		return value instanceof Number number ? number.intValue() : 1;
	}

	private static CompletableFuture<Suggestions> suggestTeams(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
		IGamePhase game = GamePhaseManager.get().getGamePhaseFor(context.getSource());
		TeamState teams = game != null ? game.instanceState().getOrNull(TeamState.KEY) : null;
		if (teams != null) {
			return SharedSuggestionProvider.suggest(teams.getTeamKeys().stream().map(GameTeamKey::id), builder);
		}
		return Suggestions.empty();
	}

	private static GameTeam getTeam(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
		String teamKey = StringArgumentType.getString(context, name);

		IGamePhase game = GamePhaseManager.get().getGamePhaseFor(context.getSource());
		TeamState teams = game != null ? game.instanceState().getOrNull(TeamState.KEY) : null;
		GameTeam team = teams != null ? teams.getTeamByKey(teamKey) : null;
		if (team == null) {
			throw NO_TEAM.create(teamKey);
		}

		return team;
	}
}
