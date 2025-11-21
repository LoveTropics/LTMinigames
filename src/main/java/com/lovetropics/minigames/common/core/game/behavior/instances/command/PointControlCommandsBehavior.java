package com.lovetropics.minigames.common.core.game.behavior.instances.command;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.command.GameCommandRegistrar;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.state.Overlords;
import com.lovetropics.minigames.common.core.game.state.statistics.Placement;
import com.lovetropics.minigames.common.core.game.state.statistics.PlacementOrder;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticsMap;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;

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
			if (source.hasPermission(Commands.LEVEL_GAMEMASTERS)) {
				return true;
			}
			ServerPlayer player = source.getPlayer();
			return player != null && overlords.contains(player);
		});

		tail
				.then(Commands.literal("list")
						.executes(context -> {
							ServerPlayer player = context.getSource().getPlayerOrException();
							Placement.fromPlayerScore(PlacementOrder.MAX, game, statistic).sendTo(PlayerSet.of(player), Integer.MAX_VALUE);
							return 1;
						})
				)
				.then(Commands.argument("player", EntityArgument.player())
						.then(Commands.literal("add")
								.then(Commands.argument("amount", IntegerArgumentType.integer())
										.executes(context -> {
											ServerPlayer player = EntityArgument.getPlayer(context, "player");
											int amount = IntegerArgumentType.getInteger(context, "amount");
											StatisticsMap statisticsMap = game.statistics().forPlayer(player);
											statisticsMap.incrementInt(statistic, amount);
											int newValue = statisticsMap.getInt(statistic);
											context.getSource().sendSuccess(() -> Component.translatable("%s now has %s %s", player.getDisplayName(), newValue, statistic.getKey()), true);
											return newValue;
										})
								)
						)
						.then(Commands.literal("set")
								.then(Commands.argument("value", IntegerArgumentType.integer())
										.executes(context -> {
											ServerPlayer player = EntityArgument.getPlayer(context, "player");
											int newValue = IntegerArgumentType.getInteger(context, "value");
											StatisticsMap statisticsMap = game.statistics().forPlayer(player);
											statisticsMap.set(statistic, newValue);
											context.getSource().sendSuccess(() -> Component.translatable("%s now has %s %s", player.getDisplayName(), newValue, statistic.getKey()), true);
											return newValue;
										})
								)
						)
						.then(Commands.literal("get")
								.executes(context -> {
									ServerPlayer player = EntityArgument.getPlayer(context, "player");
									int value = game.statistics().forPlayer(player).getInt(statistic);
									context.getSource().sendSuccess(() -> Component.translatable("%s has %s %s", player.getDisplayName(), value, statistic.getKey()), true);
									return value;
								})
						)
				);

		LiteralArgumentBuilder<CommandSourceStack> head = tail;
		for (int i = subcommand.size() - 2; i >= 0; i--) {
			head = Commands.literal(subcommand.get(i)).then(head);
		}
		commands.register(head);
	}
}
