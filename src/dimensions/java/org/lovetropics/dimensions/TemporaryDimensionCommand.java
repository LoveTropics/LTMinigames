package org.lovetropics.dimensions;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@EventBusSubscriber(modid = LTDimensionsMod.ID)
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

		List<ServerLevel> temporaryLevels = new ArrayList<>();
		for (ServerLevel level : server.getAllLevels()) {
			if (runtimeDimensions.isTemporaryDimension(level)) {
				temporaryLevels.add(level);
			}
		}

		if (temporaryLevels.isEmpty()) {
			ctx.getSource().sendSuccess(() -> Component.literal("No temporary dimensions open!"), false);
			return Command.SINGLE_SUCCESS;
		}

		for (ServerLevel level : temporaryLevels) {
			ctx.getSource().sendSuccess(() -> Component.literal(level.dimension().identifier() + ": " + level.players().size() + " players"), false);
		}

		return Command.SINGLE_SUCCESS;
	}

	private static int closeDimension(CommandContext<CommandSourceStack> ctx, boolean force) throws CommandSyntaxException {
		MinecraftServer server = ctx.getSource().getServer();
		RuntimeDimensions runtimeDimensions = RuntimeDimensions.get(server);

		ServerLevel level = DimensionArgument.getDimension(ctx, "dimension");

		RuntimeDimensionHandle handle = runtimeDimensions.asHandle(level);
		if (handle == null || !runtimeDimensions.isTemporaryDimension(level)) {
			throw NOT_TEMPORARY_DIMENSION.create(level.dimension().identifier());
		}

		if (!force && !level.players().isEmpty()) {
			throw DIMENSION_HAS_PLAYERS.create(level.dimension().identifier());
		}

		handle.markForDeletion();

		ctx.getSource().sendSuccess(() -> Component.literal("Closed '" + level.dimension().identifier() + "'"), false);

		return Command.SINGLE_SUCCESS;
	}
}
