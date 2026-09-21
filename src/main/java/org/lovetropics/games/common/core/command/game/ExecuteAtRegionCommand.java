package org.lovetropics.games.common.core.command.game;

import com.lovetropics.lib.BlockBox;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.core.game.IGameLookup;
import org.lovetropics.games.common.core.game.IGamePhase;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@EventBusSubscriber(modid = LoveTropics.ID)
public class ExecuteAtRegionCommand {
	@SubscribeEvent
	public static void register(RegisterCommandsEvent event) {
		CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
		LiteralCommandNode<CommandSourceStack> executeRoot = (LiteralCommandNode<CommandSourceStack>) dispatcher.findNode(List.of("execute"));
		dispatcher.register(
				literal("execute").then(literal("atregion")
						.then(argument("region", string())
								.fork(executeRoot, context -> {
									String regionKey = StringArgumentType.getString(context, "region");
									IGamePhase game = IGameLookup.get().getGamePhaseFor(context.getSource());
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
