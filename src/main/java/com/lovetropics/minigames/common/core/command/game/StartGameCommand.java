package com.lovetropics.minigames.common.core.command.game;

import com.lovetropics.minigames.common.core.command.argument.GameConfigArgument;
import com.lovetropics.minigames.common.core.game.IGameDefinition;
import com.lovetropics.minigames.common.core.game.config.GameConfig;
import com.lovetropics.minigames.common.core.game.impl.GameLobby;
import com.lovetropics.minigames.common.core.game.impl.GameLobbyManager;
import com.lovetropics.minigames.common.core.game.lobby.LobbyControls;
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

public class StartGameCommand {
	private static final SimpleCommandExceptionType NOT_IN_LOBBY = new SimpleCommandExceptionType(GameTexts.Commands.NOT_IN_LOBBY);
	private static final SimpleCommandExceptionType CANNOT_START_LOBBY = new SimpleCommandExceptionType(GameTexts.Commands.CANNOT_START_LOBBY);

	public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(literal("game")
				.then(literal("start").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.executes(StartGameCommand::start)
						.then(GameConfigArgument.argument("game")
								.executes(context -> enqueueAndStart(context, GameConfigArgument.get(context, "game")))
						)
				)
		);
	}

	private static int start(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		GameLobby lobby = GameLobbyManager.get().getLobbyFor(context.getSource());
		if (lobby == null) {
			throw NOT_IN_LOBBY.create();
		}
		return startLobby(context, lobby);
	}

	private static int enqueueAndStart(CommandContext<CommandSourceStack> context, GameConfig game) throws CommandSyntaxException {
		GameLobby lobby = GameLobbyManager.get().getLobbyFor(context.getSource());
		if (lobby == null) {
			ServerPlayer player = context.getSource().getPlayer();
			if (player == null) {
				throw NOT_IN_LOBBY.create();
			}
			lobby = GameLobbyManager.get().createGameLobby(player.getScoreboardName() + "'s Lobby", player).orElseThrow();
			lobby.getPlayers().joinAndPrompt(player);
		}
		lobby.getGameQueue().enqueue(game);
		return startLobby(context, lobby);
	}

	private static int startLobby(CommandContext<CommandSourceStack> context, GameLobby lobby) throws CommandSyntaxException {
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
