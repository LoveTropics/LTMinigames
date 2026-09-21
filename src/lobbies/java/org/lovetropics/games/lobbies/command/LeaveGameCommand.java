package org.lovetropics.games.lobbies.command;

import org.lovetropics.games.common.core.command.game.GameCommand;
import org.lovetropics.games.common.core.game.GameResult;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.lovetropics.games.lobbies.GameLobby;
import org.lovetropics.games.lobbies.GameLobbyManager;
import org.lovetropics.games.lobbies.GameLobbyTexts;
import org.lovetropics.games.lobbies.LobbiesMod;

import java.util.Collection;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@EventBusSubscriber(modid = LobbiesMod.ID)
public class LeaveGameCommand {
	@SubscribeEvent
	public static void register(RegisterCommandsEvent event) {
		event.getDispatcher().register(
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
						return GameResult.ok(GameLobbyTexts.Commands.leftLobby(lobby));
					}
					return GameResult.error(GameLobbyTexts.Commands.NOT_IN_LOBBY);
				}, c.getSource()));
	}

	private static int kickPlayers(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players) throws CommandSyntaxException {
		GameLobby lobby = GameLobbyManager.get().getLobbyFor(context.getSource());
		if (lobby == null) {
			throw new SimpleCommandExceptionType(GameLobbyTexts.Commands.NOT_IN_LOBBY).create();
		}
		for (ServerPlayer player : players) {
			if (lobby.getPlayers().remove(player, false)) {
				context.getSource().sendSuccess(() -> GameLobbyTexts.Commands.playerKicked(player), true);
			}
		}
		return players.size();
	}
}
