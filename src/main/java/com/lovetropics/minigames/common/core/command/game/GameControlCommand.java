package com.lovetropics.minigames.common.core.command.game;

import com.lovetropics.minigames.common.core.game.impl.GamePhaseManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class GameControlCommand {
	public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
		// @formatter:off
        dispatcher.register(
            literal("game")
                .then(argument("control", StringArgumentType.string())
                    .suggests((context, builder) -> {
                        CommandSourceStack source = context.getSource();
                        return SharedSuggestionProvider.suggest(GamePhaseManager.get().getControlInvoker(source).list(source), builder);
                    })
                    .executes(ctx -> {
                        String control = StringArgumentType.getString(ctx, "control");
                        CommandSourceStack source = ctx.getSource();
                        GamePhaseManager.get().getControlInvoker(source).invoke(control, source);
                        return Command.SINGLE_SUCCESS;
                    })
                )
        );
        // @formatter:on
	}
}
