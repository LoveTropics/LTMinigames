package com.lovetropics.minigames.common.core.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.commands.BossBarCommands;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class ExtendedBossBarCommand {
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		// @formatter:off
		dispatcher.register(
			literal("bossbar")
				.then(literal("players").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.then(literal("add")
					.then(argument("id", IdentifierArgument.id()).suggests(BossBarCommands.SUGGEST_BOSS_BAR)
					.then(argument("players", EntityArgument.players())
						.executes(ExtendedBossBarCommand::addPlayers)
					)))
					.then(literal("remove")
					.then(argument("id", IdentifierArgument.id()).suggests(BossBarCommands.SUGGEST_BOSS_BAR)
					.then(argument("players", EntityArgument.players())
						.executes(ExtendedBossBarCommand::removePlayers)
					)))
			)
		);
		// @formatter:on
	}

	private static int addPlayers(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return updatePlayers(context, CustomBossEvent::addPlayer);
	}

	private static int removePlayers(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return updatePlayers(context, CustomBossEvent::removePlayer);
	}

	private static int updatePlayers(CommandContext<CommandSourceStack> context, PlayerUpdateFunction update) throws CommandSyntaxException {
		CustomBossEvent bossBar = BossBarCommands.getBossBar(context);
		Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");

		for (ServerPlayer player : players) {
			update.apply(bossBar, player);
		}

		return Command.SINGLE_SUCCESS;
	}

	interface PlayerUpdateFunction {
		void apply(CustomBossEvent bossBar, ServerPlayer player);
	}
}
