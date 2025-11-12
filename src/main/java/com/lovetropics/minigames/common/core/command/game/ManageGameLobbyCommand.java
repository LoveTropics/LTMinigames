package com.lovetropics.minigames.common.core.command.game;

import com.lovetropics.minigames.common.core.command.argument.GameLobbyArgument;
import com.lovetropics.minigames.common.core.game.GameResult;
import com.lovetropics.minigames.common.core.game.IGameManager;
import com.lovetropics.minigames.common.core.game.lobby.IGameLobby;
import com.lovetropics.minigames.common.core.game.lobby.ILobbyManagement;
import com.lovetropics.minigames.common.core.game.util.GameTexts;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.literal;

public class ManageGameLobbyCommand {
	private static final SimpleCommandExceptionType NO_MANAGE_PERMISSION = new SimpleCommandExceptionType(GameTexts.Commands.NO_MANAGE_PERMISSION);
	private static final SimpleCommandExceptionType NOT_IN_LOBBY = new SimpleCommandExceptionType(GameTexts.Commands.NOT_IN_LOBBY);

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		// @formatter:off
		dispatcher.register(
				literal("game")
						.then(literal("create")
								.requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.executes(ManageGameLobbyCommand::createLobby)
						)
						.then(literal("manage")
								.requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.then(GameLobbyArgument.argument("lobby")
										.executes(ManageGameLobbyCommand::manageLobby)
								))
						.then(literal("manage")
								.requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.executes(ManageGameLobbyCommand::manageCurrentLobby)
						)
						.then(literal("close")
								.requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.then(GameLobbyArgument.argument("lobby")
										.executes(ManageGameLobbyCommand::closeLobby)
								))
		);
		// @formatter:on
	}

	public static GameResult<IGameLobby> createAndJoinLobby(ServerPlayer player) {
		String name = player.getScoreboardName() + "'s Lobby";
		GameResult<IGameLobby> result = IGameManager.get().createGameLobby(name, player);
		if (result.isError()) {
			return result.castError();
		}
		IGameLobby lobby = result.getOk();
		lobby.getPlayers().joinAndPrompt(player).thenAcceptAsync($ -> {
			lobby.getManagement().startManaging(player);
		}, lobby.getServer());
		return result;
	}

	private static int createLobby(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		createAndJoinLobby(player).orElseThrow();
		return Command.SINGLE_SUCCESS;
	}

	private static int manageCurrentLobby(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		IGameLobby lobby = IGameManager.get().getLobbyFor(player);
		if (lobby == null) {
			throw NOT_IN_LOBBY.create();
		}

		if (!lobby.getManagement().startManaging(player)) {
			throw NO_MANAGE_PERMISSION.create();
		}

		return Command.SINGLE_SUCCESS;
	}

	private static int manageLobby(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		IGameLobby lobby = GameLobbyArgument.get(context, "lobby");

		if (!lobby.getManagement().startManaging(player)) {
			throw NO_MANAGE_PERMISSION.create();
		}

		return Command.SINGLE_SUCCESS;
	}

	private static int closeLobby(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		IGameLobby lobby = GameLobbyArgument.get(context, "lobby");

		ILobbyManagement management = lobby.getManagement();
		if (management.canManage(context.getSource())) {
			management.close();
		} else {
			throw NO_MANAGE_PERMISSION.create();
		}

		return Command.SINGLE_SUCCESS;
	}
}
