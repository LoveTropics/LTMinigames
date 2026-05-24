package com.lovetropics.minigames.common.core.command;

import com.lovetropics.minigames.common.core.dimension.RuntimeDimensionHandle;
import com.lovetropics.minigames.common.core.dimension.RuntimeDimensions;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class TemporaryDimensionCommand {
	private static final DynamicCommandExceptionType NOT_TEMPORARY_DIMENSION = new DynamicCommandExceptionType(o ->
			Component.literal("Not a temporary dimension: '" + o + "'"));

	private static final DynamicCommandExceptionType DIMENSION_HAS_PLAYERS = new DynamicCommandExceptionType(o ->
			Component.literal("'" + o + "' contains players, use '/temporary-dimension close " + o + " force' to close anyway."));

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		// @formatter:off
        dispatcher.register(
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

		for (var dimension : runtimeDimensions.getTemporaryDimensions()) {
			ServerLevel world = server.getLevel(dimension);
			if (world == null) {
				continue;
			}
			ctx.getSource().sendSuccess(() -> Component.literal(dimension.identifier() + ": " + world.players().size() + " players"), false);
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
