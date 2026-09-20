package org.lovetropics.games.lobbies.command;

import org.lovetropics.games.common.core.game.GameResult;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.lovetropics.games.lobbies.GameLobby;
import org.lovetropics.games.lobbies.GameLobbyManager;
import org.lovetropics.games.lobbies.GameLobbyTexts;
import org.lovetropics.games.lobbies.LobbyManagement;
import org.lovetropics.games.lobbies.command.argument.GameLobbyArgument;

import static net.minecraft.commands.Commands.literal;

@EventBusSubscriber
public class ManageGameLobbyCommand {
	private static final SimpleCommandExceptionType NO_MANAGE_PERMISSION = new SimpleCommandExceptionType(GameLobbyTexts.Commands.NO_MANAGE_PERMISSION);
	private static final SimpleCommandExceptionType NOT_IN_LOBBY = new SimpleCommandExceptionType(GameLobbyTexts.Commands.NOT_IN_LOBBY);

	@SubscribeEvent
	public static void register(RegisterCommandsEvent event) {
		// @formatter:off
		event.getDispatcher().register(
				literal("game")
						.then(literal("create")
								.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.executes(ManageGameLobbyCommand::createLobby)
						)
						.then(literal("manage")
								.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.then(GameLobbyArgument.argument("lobby")
										.executes(ManageGameLobbyCommand::manageLobby)
								))
						.then(literal("manage")
								.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.executes(ManageGameLobbyCommand::manageCurrentLobby)
						)
						.then(literal("close")
								.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.then(GameLobbyArgument.argument("lobby")
										.executes(ManageGameLobbyCommand::closeLobby)
								))
		);
		// @formatter:on
	}

	public static GameResult<GameLobby> createAndJoinLobby(ServerPlayer player) {
		String name = player.getScoreboardName() + "'s Lobby";
		GameResult<GameLobby> result = GameLobbyManager.get().createGameLobby(name, player);
		if (result.isError()) {
			return result.castError();
		}
		GameLobby lobby = result.getOk();
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
		GameLobby lobby = GameLobbyManager.get().getLobbyFor(player);
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
		GameLobby lobby = GameLobbyArgument.get(context, "lobby");

		if (!lobby.getManagement().startManaging(player)) {
			throw NO_MANAGE_PERMISSION.create();
		}

		return Command.SINGLE_SUCCESS;
	}

	private static int closeLobby(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		GameLobby lobby = GameLobbyArgument.get(context, "lobby");

		LobbyManagement management = lobby.getManagement();
		if (management.canManage(context.getSource())) {
			management.close();
		} else {
			throw NO_MANAGE_PERMISSION.create();
		}

		return Command.SINGLE_SUCCESS;
	}
}
