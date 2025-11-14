package com.lovetropics.minigames.common.core.command.game;

import com.lovetropics.minigames.common.core.game.GameResult;
import com.lovetropics.minigames.common.core.game.impl.GameLobby;
import com.lovetropics.minigames.common.core.game.impl.GameLobbyManager;
import com.lovetropics.minigames.common.core.game.util.GameTexts;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.literal;

public class LeaveGameCommand {
	public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
				literal("game")
						.then(unregisterBuilder("unregister"))
						.then(unregisterBuilder("leave"))
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
}
