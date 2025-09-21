package com.lovetropics.minigames.common.core.command.game;

import com.lovetropics.minigames.common.core.game.GameResult;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

/**
 * Helper class for minigame commands.
 */
public class GameCommand {
	/**
	 * Default logic for executing a minigame action. Fetches the ActionResult back from
	 * the minigame manager actions and dispatches them as messages to command source.
	 *
	 * @param action The action executed (usually from MinigameManager)
	 * @param source The source of the executing command.
	 * @return The result of the execution (0 == fail, 1 == success)
	 */
	public static int executeGameAction(CommandAction action, CommandSourceStack source) throws CommandSyntaxException {
		GameResult<Component> result;
		try {
			result = action.run();
		} catch (CommandSyntaxException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			source.sendFailure(Component.literal(e.toString()));
			return 0;
		}

		if (result.isError()) {
			source.sendFailure(result.getError());
			return 0;
		} else {
			source.sendSuccess(result::getOk, false);
		}

		return Command.SINGLE_SUCCESS;
	}

	public interface CommandAction {
		GameResult<Component> run() throws CommandSyntaxException;
	}
}
