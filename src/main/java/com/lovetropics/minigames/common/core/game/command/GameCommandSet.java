package com.lovetropics.minigames.common.core.game.command;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public record GameCommandSet(
		CommandDispatcher<CommandSourceStack> dispatcher,
		LiteralCommandNode<CommandSourceStack> baseCommand
) {
	public static GameCommandSet registerFor(IGamePhase game) {
		LiteralArgumentBuilder<CommandSourceStack> baseCommand = Commands.literal("game");
		CommandBuildContext buildContext = CommandBuildContext.simple(game.registryAccess(), game.level().enabledFeatures());
		game.invoker(GamePhaseEvents.REGISTER_COMMANDS).register(baseCommand::then, buildContext);

		CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
		return new GameCommandSet(dispatcher, dispatcher.register(baseCommand));
	}
}
