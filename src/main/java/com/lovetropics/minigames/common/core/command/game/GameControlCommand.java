package com.lovetropics.minigames.common.core.command.game;

import com.lovetropics.minigames.common.core.game.IGameManager;
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
                        IGameManager manager = IGameManager.get();
                        CommandSourceStack source = context.getSource();
                        return SharedSuggestionProvider.suggest(manager.getControlInvoker(source).list(source), builder);
                    })
                    .executes(ctx -> {
                        String control = StringArgumentType.getString(ctx, "control");
                        IGameManager manager = IGameManager.get();
                        CommandSourceStack source = ctx.getSource();
                        manager.getControlInvoker(source).invoke(control,  source);
                        return Command.SINGLE_SUCCESS;
                    })
                )
        );
        // @formatter:on
	}
}
