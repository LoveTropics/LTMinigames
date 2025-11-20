package com.lovetropics.minigames.common.core.game.command;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public record GameCommandSet(
		CommandDispatcher<CommandSourceStack> dispatcher,
		LiteralCommandNode<CommandSourceStack> baseCommand
) {
	public static final GameCommandSet EMPTY = new GameCommandSet(
			new CommandDispatcher<>(),
			Commands.literal("game").build()
	);

	public static GameCommandSet registerFor(IGamePhase game) {
		CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
		LiteralCommandNode<CommandSourceStack> baseCommand = dispatcher.register(Commands.literal("game"));

		CommandBuildContext buildContext = CommandBuildContext.simple(game.registryAccess(), game.level().enabledFeatures());
		game.invoker(GamePhaseEvents.REGISTER_COMMANDS).register(subcommand ->
				dispatcher.register(Commands.literal("game").then(subcommand)),
				buildContext
		);

		return new GameCommandSet(dispatcher, baseCommand);
	}
}
