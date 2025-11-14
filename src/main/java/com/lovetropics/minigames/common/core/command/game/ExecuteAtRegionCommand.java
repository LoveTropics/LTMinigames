package com.lovetropics.minigames.common.core.command.game;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.impl.GamePhaseManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class ExecuteAtRegionCommand {
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralCommandNode<CommandSourceStack> executeRoot = (LiteralCommandNode<CommandSourceStack>) dispatcher.findNode(List.of("execute"));
		dispatcher.register(
				literal("execute").then(literal("atregion")
						.then(argument("region", string())
								.fork(executeRoot, context -> {
									String regionKey = StringArgumentType.getString(context, "region");
									IGamePhase game = GamePhaseManager.get().getGamePhaseFor(context.getSource());
									if (game == null) {
										return List.of();
									}
									Collection<BlockBox> regions = game.mapRegions().get(regionKey);
									List<CommandSourceStack> sources = new ArrayList<>(regions.size());
									for (BlockBox box : regions) {
										sources.add(context.getSource().withPosition(box.center()));
									}
									return sources;
								})
						)
				)
		);
	}
}
