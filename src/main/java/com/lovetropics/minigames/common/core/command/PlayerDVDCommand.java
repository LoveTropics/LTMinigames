package com.lovetropics.minigames.common.core.command;

import com.google.common.collect.Iterables;
import com.lovetropics.minigames.common.core.game.player.PlayerIterable;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.players.NameAndId;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.arguments.EntityArgument.players;
import static net.minecraft.commands.arguments.TimeArgument.time;

@EventBusSubscriber
public class PlayerDVDCommand {

	@SubscribeEvent
	public static void register(RegisterCommandsEvent event) {
		event.getDispatcher().register(literal("player_face_dvd")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(literal("add")
						.then(argument("players", players())
							.then(argument("player", GameProfileArgument.gameProfile())
								.then(argument("duration", time(0))
										.executes(context -> {
											PlayerIterable players = PlayerIterable.from(EntityArgument.getPlayers(context, "players"));
											NameAndId nameAndId = Iterables.getOnlyElement(GameProfileArgument.getGameProfiles(context, "player"));
											int duration = getInteger(context, "duration");
											players.addPlayerFaceDVD(nameAndId.id(), duration);
											return 1;
										})
								)
							)
						)
				)
				.then(literal("clear")
						.then(argument("players", players())
								.executes(context -> {
									PlayerIterable players = PlayerIterable.from(EntityArgument.getPlayers(context, "players"));
									players.clearPlayerFaceDVD();
									return 1;
								})
						)
				)
		);
	}
}
