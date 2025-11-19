package com.lovetropics.minigames.common.core.command.game;

import com.lovetropics.minigames.common.core.command.argument.PlayerRoleArgument;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.impl.GamePhaseManager;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.util.GameTexts;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class GameSetRoleCommand {
	private static final SimpleCommandExceptionType NOT_IN_GAME = new SimpleCommandExceptionType(GameTexts.Commands.NOT_IN_GAME);

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(literal("game").then(literal("setrole")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(argument("players", EntityArgument.players())
						.then(PlayerRoleArgument.argument("role")
								.executes(context ->
										setRole(context, EntityArgument.getPlayers(context, "players"), PlayerRoleArgument.get(context, "role"))
								)
						)
				)
		));
	}

	private static int setRole(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players, PlayerRole role) throws CommandSyntaxException {
		IGamePhase game = GamePhaseManager.get().getGamePhaseFor(context.getSource());
		if (game == null) {
			throw NOT_IN_GAME.create();
		}
		for (ServerPlayer player : players) {
			if (game.allPlayers().contains(player)) {
				game.setPlayerRole(player, role);
			} else {
				context.getSource().sendFailure(Component.translatable("%s is in a different game", player.getDisplayName()));
			}
		}
		return 1;
	}
}
