package com.lovetropics.minigames.common.core.command.game;

import com.lovetropics.minigames.common.core.game.GameResult;
import com.lovetropics.minigames.common.core.game.GameStopReason;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.event.GameLogicEvents;
import com.lovetropics.minigames.common.core.game.impl.GameManager;
import com.lovetropics.minigames.common.core.game.util.GameTexts;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

import static net.minecraft.commands.Commands.literal;

public class FinishGameCommand {
	public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
				literal("game")
						.then(literal("finish")
								.executes(c -> GameCommand.executeGameAction(() -> {
									IGamePhase game = GameManager.get().getGamePhaseFor(c.getSource());
									if (game == null) {
										return GameResult.error(GameTexts.Commands.NOT_IN_GAME);
									}
									game.invoker(GameLogicEvents.REQUEST_GAME_OVER).requestGameOver();
									return game.requestStop(GameStopReason.finished()).map($ -> {
										return GameTexts.Commands.stoppedGame(game.definition());
									});
								}, c.getSource())))
		);
	}
}
