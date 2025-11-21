package com.lovetropics.minigames.common.core.command;

import com.lovetropics.minigames.common.core.game.player.PlayerIterable;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.arguments.EntityArgument.players;
import static net.minecraft.commands.arguments.TimeArgument.time;

@EventBusSubscriber
public class FadeToBlackCommand {
	@SubscribeEvent
	public static void register(RegisterCommandsEvent event) {
		event.getDispatcher().register(literal("fadetoblack")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(literal("in")
						.then(argument("players", players())
								.then(argument("duration", time(0))
										.executes(context -> {
											PlayerIterable players = PlayerIterable.from(EntityArgument.getPlayers(context, "players"));
											int duration = getInteger(context, "duration");
											players.fadeToBlack(duration);
											return 1;
										})
								)
						)
				)
				.then(literal("out")
						.then(argument("players", players())
								.then(argument("duration", time(0))
										.executes(context -> {
											PlayerIterable players = PlayerIterable.from(EntityArgument.getPlayers(context, "players"));
											int duration = getInteger(context, "duration");
											players.fadeFromBlack(duration);
											return 1;
										})
								)
						)
				)
		);
	}
}
