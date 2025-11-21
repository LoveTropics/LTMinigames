package com.lovetropics.minigames.common.core.command.game;

import com.lovetropics.minigames.common.core.game.persistent.behavior.crab.CrabGolfWinBehavior;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class GolfCommand {
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(literal("persistentgame")
			.then(literal("golf").requires(s -> s.hasPermission(2))
				.then(literal("highscore")
					.then(argument("hole", IntegerArgumentType.integer())
						.then(argument("target", EntityArgument.player())
							.executes(ctx -> {
								int hole = IntegerArgumentType.getInteger(ctx, "hole");
								ServerPlayer player = EntityArgument.getPlayer(ctx, "target");

								int score = CrabGolfWinBehavior.GolfData.get(ctx.getSource().getLevel()).getHighScoreFor(hole, player);

								return score;
							})
						)
					)
				)
				.then(literal("clear")
					.then(argument("hole", IntegerArgumentType.integer())
						.executes(ctx -> {
							int hole = IntegerArgumentType.getInteger(ctx, "hole");

							CommandSourceStack source = ctx.getSource();
							CrabGolfWinBehavior.GolfData.get(source.getLevel()).clearHole(hole);

							return 0;
						})
					)
				)
			)
		);
	}
}
