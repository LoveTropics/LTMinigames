package com.lovetropics.minigames.common.core.game.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

@FunctionalInterface
public interface GameCommandRegistrar {
	default void registerAdmin(String name, SimpleAction action) {
		register(Commands.literal(name)
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(context -> {
					action.execute(context.getSource());
					return 1;
				})
		);
	}

	void register(LiteralArgumentBuilder<CommandSourceStack> subcommand);

	interface SimpleAction {
		void execute(CommandSourceStack source) throws CommandSyntaxException;
	}
}
