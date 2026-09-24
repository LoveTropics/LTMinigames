package org.lovetropics.games.common.core.game.command;

import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import java.util.List;

public record GameCommandSet(
		CommandDispatcher<CommandSourceStack> dispatcher,
		LiteralCommandNode<CommandSourceStack> baseCommand
) {
	public static final GameCommandSet EMPTY = new GameCommandSet(
			new CommandDispatcher<>(),
			Commands.literal("game").build()
	);

	/// @param phases the phase to register commands for, followed by its parent phases - players in a sub-phase can still use the commands of its parents
	public static GameCommandSet registerFor(List<? extends IGamePhase> phases) {
		CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
		LiteralCommandNode<CommandSourceStack> baseCommand = dispatcher.register(Commands.literal("game"));

		IGamePhase game = phases.getFirst();
		CommandBuildContext buildContext = CommandBuildContext.simple(game.registryAccess(), game.server().getWorldData().enabledFeatures());
		// Outermost first, so that the commands of a sub-phase take priority over any of its parents with the same name
		for (IGamePhase phase : phases.reversed()) {
			phase.invoker(GamePhaseEvents.REGISTER_COMMANDS).register(subcommand ->
					dispatcher.register(Commands.literal("game").then(subcommand)),
					buildContext
			);
		}

		return new GameCommandSet(dispatcher, baseCommand);
	}
}
