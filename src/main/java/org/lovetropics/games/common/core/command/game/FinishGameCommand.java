package org.lovetropics.games.common.core.command.game;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.core.game.GameResult;
import org.lovetropics.games.common.core.game.GameStopReason;
import org.lovetropics.games.common.core.game.IGameLookup;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.event.GameLogicEvents;
import org.lovetropics.games.common.core.game.util.GameTexts;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import static net.minecraft.commands.Commands.literal;

@EventBusSubscriber(modid = LoveTropics.ID)
public class FinishGameCommand {
	public static void register(RegisterCommandsEvent event) {
		event.getDispatcher().register(
				literal("game")
						.then(literal("finish")
								.executes(c -> GameCommand.executeGameAction(() -> {
									IGamePhase game = IGameLookup.get().getGamePhaseFor(c.getSource());
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
