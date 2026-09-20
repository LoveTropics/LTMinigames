package com.lovetropics.minigames.common.core.command.game;

import com.lovetropics.minigames.common.core.game.GameResult;
import com.lovetropics.minigames.common.core.game.GameStopReason;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.event.GameLogicEvents;
import com.lovetropics.minigames.common.core.game.impl.GamePhaseManager;
import com.lovetropics.minigames.common.core.game.util.GameTexts;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import static net.minecraft.commands.Commands.literal;

@EventBusSubscriber
public class FinishGameCommand {
	public static void register(RegisterCommandsEvent event) {
		event.getDispatcher().register(
				literal("game")
						.then(literal("finish")
								.executes(c -> GameCommand.executeGameAction(() -> {
									IGamePhase game = GamePhaseManager.get().getGamePhaseFor(c.getSource());
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
