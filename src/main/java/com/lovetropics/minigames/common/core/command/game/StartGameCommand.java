package com.lovetropics.minigames.common.core.command.game;

import com.lovetropics.minigames.common.core.command.argument.GameConfigArgument;
import com.lovetropics.minigames.common.core.game.IGameDefinition;
import com.lovetropics.minigames.common.core.game.IGameManager;
import com.lovetropics.minigames.common.core.game.lobby.IGameLobby;
import com.lovetropics.minigames.common.core.game.lobby.LobbyControls;
import com.lovetropics.minigames.common.core.game.util.GameTexts;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import static net.minecraft.commands.Commands.literal;

public class StartGameCommand {
	private static final SimpleCommandExceptionType NOT_IN_LOBBY = new SimpleCommandExceptionType(GameTexts.Commands.NOT_IN_LOBBY);
	private static final SimpleCommandExceptionType CANNOT_START_LOBBY = new SimpleCommandExceptionType(GameTexts.Commands.CANNOT_START_LOBBY);

	public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(literal("game")
				.then(literal("start").requires(s -> s.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.executes(StartGameCommand::start)
						.then(GameConfigArgument.argument("game")
								.executes(StartGameCommand::enqueueAndStart)
						)
				)
		);
	}

	private static int start(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		IGameLobby lobby = IGameManager.get().getLobbyFor(context.getSource());
		if (lobby == null) {
			throw NOT_IN_LOBBY.create();
		}
		return startLobby(context, lobby);
	}

	private static int enqueueAndStart(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		IGameLobby lobby = IGameManager.get().getLobbyFor(context.getSource());
		if (lobby == null) {
			throw NOT_IN_LOBBY.create();
		}
		lobby.getGameQueue().enqueue(GameConfigArgument.get(context, "game"));
		return startLobby(context, lobby);
	}

	private static int startLobby(CommandContext<CommandSourceStack> context, IGameLobby lobby) throws CommandSyntaxException {
		LobbyControls.Action action = lobby.getControls().get(LobbyControls.Type.PLAY);
		if (action == null) {
			throw CANNOT_START_LOBBY.create();
		}

		action.run().orElseThrow();
		IGameDefinition game = lobby.getCurrentGameDefinition();
		if (game != null) {
			context.getSource().sendSuccess(() -> GameTexts.Commands.startedGame(game), false);
		}

		return Command.SINGLE_SUCCESS;
	}
}
