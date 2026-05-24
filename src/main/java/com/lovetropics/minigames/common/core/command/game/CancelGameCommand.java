package com.lovetropics.minigames.common.core.command.game;

import com.lovetropics.minigames.common.core.game.GameResult;
import com.lovetropics.minigames.common.core.game.GameStopReason;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.impl.GamePhaseManager;
import com.lovetropics.minigames.common.core.game.util.GameTexts;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

import static net.minecraft.commands.Commands.literal;

public class CancelGameCommand {
	public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
		// @formatter:off
		dispatcher.register(
				literal("game")
						.then(literal("cancel").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.executes(context -> cancel(context, false))
								.then(literal("confirm")
										.executes(context -> cancel(context, true))
								)
						)
		);
		// @formatter:on
	}

	private static int cancel(CommandContext<CommandSourceStack> ctx, boolean confirmed) throws CommandSyntaxException {
		return GameCommand.executeGameAction(() -> {
			IGamePhase game = GamePhaseManager.get().getGamePhaseFor(ctx.getSource());
			if (game == null) {
				return GameResult.error(GameTexts.Commands.NOT_IN_GAME);
			}

			if (!confirmed && shouldRequireConfirmation(ctx.getSource(), game)) {
				MutableComponent message = Component.literal("Please confirm that you would like to cancel this game! ")
						.append(GameTexts.clickHere("/game cancel confirm"));
				return GameResult.error(message);
			}

			return game.requestStop(GameStopReason.canceled()).map(u -> GameTexts.Commands.stoppedGame(game.definition()));
		}, ctx.getSource());
	}

	private static boolean shouldRequireConfirmation(CommandSourceStack source, IGamePhase game) {
		return source.getEntity() instanceof Player && game.allPlayers().size() > 1;
	}
}
