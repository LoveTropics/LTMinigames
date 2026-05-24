package com.lovetropics.minigames.common.core.command.game;

import com.lovetropics.minigames.common.core.game.GameResult;
import com.lovetropics.minigames.common.core.game.impl.GameLobby;
import com.lovetropics.minigames.common.core.game.impl.GameLobbyManager;
import com.lovetropics.minigames.common.core.game.util.GameTexts;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class LeaveGameCommand {
	public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
				literal("game")
						.then(unregisterBuilder("unregister"))
						.then(unregisterBuilder("leave"))
						.then(literal("kick")
								.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.then(argument("players", EntityArgument.players())
										.executes(context ->
												kickPlayers(context, EntityArgument.getPlayers(context, "players"))
										)
								)
						)
		);
	}

	private static LiteralArgumentBuilder<CommandSourceStack> unregisterBuilder(String name) {
		return literal(name).requires(s -> s.getEntity() instanceof ServerPlayer)
				.executes(c -> GameCommand.executeGameAction(() -> {
					CommandSourceStack source = c.getSource();
					GameLobby lobby = GameLobbyManager.get().getLobbyFor(source);
					if (lobby != null && lobby.getPlayers().remove(source.getPlayerOrException(), false)) {
						return GameResult.ok(GameTexts.Commands.leftLobby(lobby));
					}
					return GameResult.error(GameTexts.Commands.NOT_IN_LOBBY);
				}, c.getSource()));
	}

	private static int kickPlayers(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players) throws CommandSyntaxException {
		GameLobby lobby = GameLobbyManager.get().getLobbyFor(context.getSource());
		if (lobby == null) {
			throw new SimpleCommandExceptionType(GameTexts.Commands.NOT_IN_LOBBY).create();
		}
		for (ServerPlayer player : players) {
			if (lobby.getPlayers().remove(player, false)) {
				context.getSource().sendSuccess(() -> GameTexts.Commands.playerKicked(player), true);
			}
		}
		return players.size();
	}
}
