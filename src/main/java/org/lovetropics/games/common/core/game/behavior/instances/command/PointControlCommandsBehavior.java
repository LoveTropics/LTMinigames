package org.lovetropics.games.common.core.game.behavior.instances.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.ExtraCodecs;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.instances.team.TeamsBehavior;
import org.lovetropics.games.common.core.game.command.GameCommandRegistrar;
import org.lovetropics.games.common.core.game.state.Overlords;
import org.lovetropics.games.common.core.game.state.statistics.Placement;
import org.lovetropics.games.common.core.game.state.statistics.PlacementOrder;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;
import org.lovetropics.games.common.core.game.state.statistics.StatisticsMap;
import org.lovetropics.games.common.core.game.state.team.GameTeam;
import org.lovetropics.games.common.core.game.state.team.TeamState;

import java.util.List;

public record PointControlCommandsBehavior(
		StatisticKey<Integer> statistic,
		List<String> subcommand
) implements IGameBehavior {
	public static final MapCodec<PointControlCommandsBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			StatisticKey.INT_CODEC.fieldOf("statistic").forGetter(PointControlCommandsBehavior::statistic),
			ExtraCodecs.nonEmptyList(ExtraCodecs.RESOURCE_PATH_CODEC.listOf()).fieldOf("subcommand").forGetter(PointControlCommandsBehavior::subcommand)
	).apply(i, PointControlCommandsBehavior::new));

	@Override
	public void register(IGamePhase topGame, EventRegistrar events) throws GameException {
		events.listen(GamePhaseEvents.REGISTER_COMMANDS, (commands, buildContext) ->
				registerCommands(topGame, commands)
		);
	}

	private void registerCommands(IGamePhase game, GameCommandRegistrar commands) {
		Overlords overlords = Overlords.get(game);

		LiteralArgumentBuilder<CommandSourceStack> tail = Commands.literal(subcommand.getLast());
		tail.requires(source -> {
			if (source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
				return true;
			}
			ServerPlayer player = source.getPlayer();
			return player != null && overlords.contains(player);
		});

		TeamState teams = game.instanceState().getOrNull(TeamState.KEY);

		tail.then(Commands.literal("player")
				.then(Commands.literal("list")
						.executes(context -> {
							Placement.fromPlayerScore(PlacementOrder.MAX, game, game.statistics(), statistic).buildDisplay(Integer.MAX_VALUE, true, true, context.getSource()::sendSystemMessage);
							return 1;
						})
				)
				.then(statisticModifier(Commands.argument("player", EntityArgument.player()), new Access() {
					@Override
					public StatisticsMap getStatisticsMap(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
						ServerPlayer player = EntityArgument.getPlayer(context, "player");
						return game.statistics().forPlayer(player);
					}

					@Override
					public Component getDisplayName(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
						ServerPlayer player = EntityArgument.getPlayer(context, "player");
						return player.getDisplayName();
					}
				}))
		);

		if (teams != null) {
			tail.then(Commands.literal("team")
					.then(Commands.literal("list")
							.executes(context -> {
								Placement.fromTeamScore(PlacementOrder.MAX, game, game.statistics(), statistic).buildDisplay(Integer.MAX_VALUE, true, true, context.getSource()::sendSystemMessage);
								return 1;
							})
					)
					.then(statisticModifier(
							Commands.argument("team", StringArgumentType.string())
									.suggests(TeamsBehavior.suggestTeam(teams)),
							new Access() {
								@Override
								public StatisticsMap getStatisticsMap(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
									GameTeam team = TeamsBehavior.getTeamArgument(context, "team", teams);
									return game.statistics().forTeam(team);
								}

								@Override
								public Component getDisplayName(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
									GameTeam team = TeamsBehavior.getTeamArgument(context, "team", teams);
									return team.styledName();
								}
							}
					))
			);
		}

		LiteralArgumentBuilder<CommandSourceStack> head = tail;
		for (int i = subcommand.size() - 2; i >= 0; i--) {
			head = Commands.literal(subcommand.get(i)).then(head);
		}
		commands.register(head);
	}

	private ArgumentBuilder<CommandSourceStack, ?> statisticModifier(ArgumentBuilder<CommandSourceStack, ?> argument, Access access) {
		return argument
				.then(Commands.literal("add")
						.then(Commands.argument("amount", IntegerArgumentType.integer())
								.executes(context -> {
									StatisticsMap statisticsMap = access.getStatisticsMap(context);
									Component displayName = access.getDisplayName(context);
									int amount = IntegerArgumentType.getInteger(context, "amount");
									statisticsMap.incrementInt(statistic, amount);
									int newValue = statisticsMap.getInt(statistic);
									context.getSource().sendSuccess(() -> Component.translatable("%s now has %s %s", displayName, newValue, statistic.getKey()), true);
									return newValue;
								})
						)
				)
				.then(Commands.literal("set")
						.then(Commands.argument("value", IntegerArgumentType.integer())
								.executes(context -> {
									StatisticsMap statisticsMap = access.getStatisticsMap(context);
									Component displayName = access.getDisplayName(context);
									int newValue = IntegerArgumentType.getInteger(context, "value");
									statisticsMap.set(statistic, newValue);
									context.getSource().sendSuccess(() -> Component.translatable("%s now has %s %s", displayName, newValue, statistic.getKey()), true);
									return newValue;
								})
						)
				)
				.then(Commands.literal("get")
						.executes(context -> {
							Component displayName = access.getDisplayName(context);
							int value = access.getStatisticsMap(context).getInt(statistic);
							context.getSource().sendSuccess(() -> Component.translatable("%s has %s %s", displayName, value, statistic.getKey()), true);
							return value;
						})
				);
	}

	private interface Access {
		StatisticsMap getStatisticsMap(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;

		Component getDisplayName(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;
	}
}
