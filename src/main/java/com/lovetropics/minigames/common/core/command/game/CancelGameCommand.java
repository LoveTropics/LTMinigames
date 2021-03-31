package com.lovetropics.minigames.common.core.command.game;

import com.lovetropics.minigames.client.data.TropicraftLangKeys;
import com.lovetropics.minigames.common.core.game.IGameInstance;
import com.lovetropics.minigames.common.core.game.SingleGameManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandSource;
import net.minecraft.util.text.TranslationTextComponent;

import static net.minecraft.command.Commands.literal;

public class CancelGameCommand {
	public static void register(final CommandDispatcher<CommandSource> dispatcher) {
		dispatcher.register(
			literal("game")
			.then(literal("cancel").requires(s -> s.hasPermissionLevel(2))
			.executes(c -> GameCommand.executeMinigameAction(() -> {
				IGameInstance game = SingleGameManager.INSTANCE.getActiveGame();
				if (game == null) {
					throw new SimpleCommandExceptionType(new TranslationTextComponent(TropicraftLangKeys.COMMAND_NO_MINIGAME)).create();
				}
				return SingleGameManager.INSTANCE.cancel(game);
			}, c.getSource())))
		);
	}
}
