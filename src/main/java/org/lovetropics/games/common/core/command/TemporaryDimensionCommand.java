package org.lovetropics.games.common.core.command;

import org.lovetropics.games.common.core.dimension.RuntimeDimensionHandle;
import org.lovetropics.games.common.core.dimension.RuntimeDimensions;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@EventBusSubscriber
public class TemporaryDimensionCommand {
	private static final DynamicCommandExceptionType NOT_TEMPORARY_DIMENSION = new DynamicCommandExceptionType(o ->
			Component.literal("Not a temporary dimension: '" + o + "'"));

	private static final DynamicCommandExceptionType DIMENSION_HAS_PLAYERS = new DynamicCommandExceptionType(o ->
			Component.literal("'" + o + "' contains players, use '/temporary-dimension close " + o + " force' to close anyway."));

	@SubscribeEvent
	public static void register(RegisterCommandsEvent event) {
		// @formatter:off
        event.getDispatcher().register(
                literal("temporary-dimension")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(literal("list")
                                .executes(TemporaryDimensionCommand::listTemporaryDimensions))
                        .then(literal("close")
                                .then(argument("dimension", DimensionArgument.dimension())
                                        .executes(ctx -> closeDimension(ctx, false))
                                        .then(literal("force")
                                                .executes(ctx -> closeDimension(ctx, true)))))
        );
        // @formatter:on
	}

	private static int listTemporaryDimensions(CommandContext<CommandSourceStack> ctx) {
		MinecraftServer server = ctx.getSource().getServer();
		RuntimeDimensions runtimeDimensions = RuntimeDimensions.get(server);

		if (runtimeDimensions.getTemporaryDimensions().isEmpty()) {
			ctx.getSource().sendSuccess(() -> Component.literal("No temporary dimensions open!"), false);
		}

		for (ResourceKey<Level> dimension : runtimeDimensions.getTemporaryDimensions()) {
			ServerLevel level = server.getLevel(dimension);
			if (level == null) {
				continue;
			}
			ctx.getSource().sendSuccess(() -> Component.literal(dimension.identifier() + ": " + level.players().size() + " players"), false);
		}

		return Command.SINGLE_SUCCESS;
	}

	private static int closeDimension(CommandContext<CommandSourceStack> ctx, boolean force) throws CommandSyntaxException {
		MinecraftServer server = ctx.getSource().getServer();
		RuntimeDimensions runtimeDimensions = RuntimeDimensions.get(server);

		ServerLevel level = DimensionArgument.getDimension(ctx, "dimension");

		if (!runtimeDimensions.isTemporaryDimension(level.dimension())) {
			throw NOT_TEMPORARY_DIMENSION.create(level.dimension().identifier());
		}

		if (!force && !level.players().isEmpty()) {
			throw DIMENSION_HAS_PLAYERS.create(level.dimension().identifier());
		}

		RuntimeDimensionHandle handle = runtimeDimensions.handleForTemporaryDimension(level.dimension());
		handle.delete();

		ctx.getSource().sendSuccess(() -> Component.literal("Closed '" + level.dimension().identifier() + "'"), false);

		return Command.SINGLE_SUCCESS;
	}
}
