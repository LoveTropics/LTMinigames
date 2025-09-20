package com.lovetropics.minigames.common.core.command.game;

import com.lovetropics.minigames.common.core.command.argument.GameLobbyArgument;
import com.lovetropics.minigames.common.core.command.argument.PlayerRoleArgument;
import com.lovetropics.minigames.common.core.game.GameResult;
import com.lovetropics.minigames.common.core.game.IGameManager;
import com.lovetropics.minigames.common.core.game.lobby.IGameLobby;
import com.lovetropics.minigames.common.core.game.lobby.IGameLobbyPlayers;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.util.GameTexts;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class JoinGameCommand {
	public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
		// @formatter:off
		dispatcher.register(
			literal("game")
				.then(joinBuilder("register"))
				.then(joinBuilder("join"))
				.then(joinBuilder("play"))
				.then(literal("force").requires(source -> source.hasPermission(Commands.LEVEL_OWNERS))
					.then(argument("player", EntityArgument.players())
					.executes(JoinGameCommand::forcePlayerJoin)
				))
		);
		// @formatter:on
	}

	private static LiteralArgumentBuilder<CommandSourceStack> joinBuilder(String name) {
		// @formatter:off
		return literal(name)
				.executes(ctx -> joinAsRole(ctx, null, null))
				.then(GameLobbyArgument.argument("lobby")
					.executes(ctx -> joinAsRole(ctx, GameLobbyArgument.get(ctx, "lobby"), null))
					.then(literal("as").requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(PlayerRoleArgument.argument("role")
						.executes(ctx -> joinAsRole(ctx, GameLobbyArgument.get(ctx, "lobby"), PlayerRoleArgument.get(ctx, "role")))
					))
				);
		// @formatter:on
	}

	private static int joinAsRole(CommandContext<CommandSourceStack> ctx, @Nullable IGameLobby givenLobby, @Nullable PlayerRole forcedRole) throws CommandSyntaxException {
		return joinAsRole(givenLobby, forcedRole, ctx.getSource().getPlayerOrException(), ctx.getSource());
	}

	public static int joinAsRole(@Nullable IGameLobby givenLobby, @Nullable PlayerRole forcedRole, ServerPlayer player, CommandSourceStack source) {
		GameResult<IGameLobby> lobbyResult = resolveLobby(source, givenLobby, forcedRole);
		if (lobbyResult.isError()) {
			source.sendFailure(lobbyResult.getError());
			return Command.SINGLE_SUCCESS;
		}

		IGameLobby lobby = lobbyResult.getOk();
		IGameLobbyPlayers players = lobby.getPlayers();

		CompletableFuture<GameResult<Unit>> joinFuture;
		if (forcedRole == null) {
			joinFuture = CompletableFuture.supplyAsync(() -> players.joinAndPrompt(player), source.getServer()).thenCompose(Function.identity());
		} else {
			joinFuture = CompletableFuture.completedFuture(players.join(player, forcedRole));
		}

		GameResult.handleException("An unexpected error has occurred", joinFuture).thenAcceptAsync(result -> {
			if (result.isOk()) {
				source.sendSuccess(() -> GameTexts.Commands.joinedLobby(lobby), false);
			} else {
				source.sendFailure(result.getError());
			}
		}, source.getServer());

		return Command.SINGLE_SUCCESS;
	}

	private static GameResult<IGameLobby> resolveLobby(CommandSourceStack source, @Nullable IGameLobby givenLobby, @Nullable PlayerRole forcedRole) {
		if (givenLobby != null) {
			return GameResult.ok(givenLobby);
		} else {
			List<? extends IGameLobby> lobbies = IGameManager.get().getVisibleLobbies(source).collect(Collectors.toList());
			if (lobbies.size() == 1) {
				return GameResult.ok(lobbies.getFirst());
			} else if (lobbies.isEmpty()) {
				return GameResult.error(GameTexts.Commands.NO_JOINABLE_LOBBIES);
			}

			return GameResult.error(GameTexts.Commands.lobbySelector(lobbies, forcedRole));
		}
	}

	private static int forcePlayerJoin(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = EntityArgument.getPlayer(context, "player");
		IGameLobby lobby = IGameManager.get().getLobbyFor(player);
		if (lobby == null) {
			throw new SimpleCommandExceptionType(GameTexts.Commands.NOT_IN_LOBBY).create();
		}

		lobby.getPlayers().forceRole(player, PlayerRole.PARTICIPANT);

		return Command.SINGLE_SUCCESS;
	}
}
