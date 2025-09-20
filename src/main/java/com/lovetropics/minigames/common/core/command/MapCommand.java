package com.lovetropics.minigames.common.core.command;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.command.argument.DimensionArgument;
import com.lovetropics.minigames.common.core.command.argument.MapWorkspaceArgument;
import com.lovetropics.minigames.common.core.dimension.DimensionUtils;
import com.lovetropics.minigames.common.core.map.MapExportReader;
import com.lovetropics.minigames.common.core.map.MapExportWriter;
import com.lovetropics.minigames.common.core.map.MapMetadata;
import com.lovetropics.minigames.common.core.map.MapRegions;
import com.lovetropics.minigames.common.core.map.VoidChunkGenerator;
import com.lovetropics.minigames.common.core.map.workspace.MapWorkspace;
import com.lovetropics.minigames.common.core.map.workspace.MapWorkspaceManager;
import com.lovetropics.minigames.common.core.map.workspace.WorkspaceDimensionConfig;
import com.lovetropics.minigames.common.core.map.workspace.WorkspacePositionTracker;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class MapCommand {
	private static final DynamicCommandExceptionType WORKSPACE_ALREADY_EXISTS = new DynamicCommandExceptionType(id ->
			Component.literal("Workspace already exists with id '" + id + "'")
	);
	private static final DynamicCommandExceptionType MAP_DOES_NOT_EXIST = new DynamicCommandExceptionType(id ->
			Component.literal("Map does not exist with id '" + id + "'")
	);

	private static final SimpleCommandExceptionType NOT_IN_WORKSPACE = new SimpleCommandExceptionType(Component.literal("You are not in a workspace!"));

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		// @formatter:off
        dispatcher.register(
            literal("map")
				.requires(source -> source.hasPermission(2))
                .then(literal("open")
                    .then(argument("id", StringArgumentType.string())
						.then(DimensionArgument.argument("dimension")
						.executes(context ->{
							LevelStem dimension = DimensionArgument.get(context, "dimension");
							return openMap(context, dimension);
						})
					)
						.executes(context -> {
							MinecraftServer server = context.getSource().getServer();
							LevelStem dimension = new LevelStem(DimensionUtils.overworld(server), new VoidChunkGenerator(server));
							return openMap(context, dimension);
						})
                ))
				.then(literal("delete")
					.then(MapWorkspaceArgument.argument("id")
					.executes(MapCommand::deleteMap)
				))
				.then(literal("join")
					.then(MapWorkspaceArgument.argument("id")
					.executes(MapCommand::joinMap)
				))
				.then(literal("leave").executes(MapCommand::leaveMap))
				.then(literal("export")
					.then(MapWorkspaceArgument.argument("id")
					.executes(MapCommand::exportMap)
				))
				.then(literal("import")
					.then(argument("location", ResourceLocationArgument.id())
							.then(DimensionArgument.argument("dimension")
							.executes(context ->{
								LevelStem dimension = DimensionArgument.get(context, "dimension");
								return importMap(context, dimension);
							})
						)
							.executes(context -> {
								MinecraftServer server = context.getSource().getServer();
								LevelStem dimension = new LevelStem(DimensionUtils.overworld(server), new VoidChunkGenerator(server));
								return importMap(context, dimension);
							})
					)
				)
				.then(literal("region")
					.then(literal("add")
						.then(argument("key", StringArgumentType.string())
								.then(argument("min", BlockPosArgument.blockPos())
								.then(argument("max", BlockPosArgument.blockPos())
								.executes(MapCommand::addRegion)
							))
								.executes(MapCommand::addRegionHere)
						)
					)
					.then(literal("hide")
							.executes(MapCommand::showHideRegions))
				)
        );
        // @formatter:on
	}

	private static int openMap(CommandContext<CommandSourceStack> context, LevelStem dimension) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		MinecraftServer server = source.getServer();
		MapWorkspaceManager workspaceManager = MapWorkspaceManager.get(server);

		String id = StringArgumentType.getString(context, "id");
		if (workspaceManager.hasWorkspace(id)) {
			throw WORKSPACE_ALREADY_EXISTS.create(id);
		}

		long seed = server.overworld().getSeed();
		WorkspaceDimensionConfig dimensionConfig = new WorkspaceDimensionConfig(dimension.type(), dimension.generator(), seed);

		workspaceManager.openWorkspace(id, dimensionConfig).thenAcceptAsync(workspace -> {
			source.sendSuccess(() -> {
				MutableComponent message = Component.literal("Opened workspace with id '" + id + "'. ").withStyle(ChatFormatting.AQUA);
				Component join = Component.literal("Click here to join")
						.withStyle(style -> {
							String command = "/map join " + id;
							return style.withColor(ChatFormatting.BLUE)
									.withUnderlined(true)
									.withClickEvent(new ClickEvent.RunCommand(command))
									.withHoverEvent(new HoverEvent.ShowText(Component.literal(command)));
						});
				return message.append(join);
			}, false);
		}, server);

		return Command.SINGLE_SUCCESS;
	}

	private static int deleteMap(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		MapWorkspaceManager workspaceManager = MapWorkspaceManager.get(source.getServer());

		MapWorkspace workspace = MapWorkspaceArgument.get(context, "id");
		workspaceManager.deleteWorkspace(workspace.id());

		source.sendSuccess(() -> Component.literal("Deleted workspace with id '" + workspace.id() + "'. ").withStyle(ChatFormatting.GOLD), false);

		return Command.SINGLE_SUCCESS;
	}

	private static int leaveMap(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();

		WorkspacePositionTracker.Position returnPosition = WorkspacePositionTracker.getReturnPositionFor(player);
		if (returnPosition != null) {
			returnPosition.applyTo(player);
		} else {
			DimensionUtils.teleportPlayerNoPortal(player, Level.OVERWORLD, new BlockPos(0, 64, 0));
		}

		return Command.SINGLE_SUCCESS;
	}

	private static int joinMap(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();

		MapWorkspace workspace = MapWorkspaceArgument.get(context, "id");

		WorkspacePositionTracker.Position position = WorkspacePositionTracker.getPositionFor(player, workspace);
		if (position != null) {
			position.applyTo(player);
		} else {
			ResourceKey<Level> dimension = workspace.dimensionKey();
			ServerLevel world = context.getSource().getServer().getLevel(dimension);
			DimensionUtils.teleportPlayerNoPortal(player, dimension, world.getSharedSpawnPos());
		}

		if (player.getAbilities().mayfly) {
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
		}

		return Command.SINGLE_SUCCESS;
	}

	private static int addRegion(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		MapWorkspace workspace = getCurrentWorkspace(context);

		String key = StringArgumentType.getString(context, "key");
		BlockPos min = BlockPosArgument.getSpawnablePos(context, "min");
		BlockPos max = BlockPosArgument.getSpawnablePos(context, "max");

		workspace.regions().add(context.getSource().getServer(), key, BlockBox.of(min, max));

		return Command.SINGLE_SUCCESS;
	}

	private static int addRegionHere(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		MapWorkspace workspace = getCurrentWorkspace(context);
		Vec3 pos = context.getSource().getPosition();

		String key = StringArgumentType.getString(context, "key");

		workspace.regions().add(context.getSource().getServer(), key, BlockBox.of(BlockPos.containing(pos)));

		return Command.SINGLE_SUCCESS;
	}

	private static int showHideRegions(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		MapWorkspace workspace = getCurrentWorkspace(context);
		workspace.regions().showHide(context.getSource().getPlayerOrException());

		return Command.SINGLE_SUCCESS;
	}

	private static int exportMap(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		MapWorkspace workspace = MapWorkspaceArgument.get(context, "id");

		MinecraftServer server = source.getServer();

		CompletableFuture<Void> saveAll = saveWorkspace(server, workspace);

		saveAll.thenRunAsync(() -> {
			LevelStorageSource.LevelStorageAccess save = server.storageSource;
			Path dimensionDirectory = save.getDimensionPath(workspace.dimensionKey());

			ResourceLocation id = LoveTropics.location(workspace.id());
			Path exportPath = MapExportWriter.pathFor(id);

			try {
				Files.createDirectories(exportPath.getParent());

				try (MapExportWriter writer = MapExportWriter.open(exportPath)) {
					MapRegions regions = workspace.regions().compile();
					writer.writeMetadata(new MapMetadata(id, workspace.worldSettings(), regions));
					writer.writeWorldData(dimensionDirectory);

					source.sendSuccess(() -> Component.literal("Successfully exported map!"), false);
				}
			} catch (Exception e) {
				source.sendFailure(Component.literal("Failed to export map!"));
				LoveTropics.LOGGER.error("Failed to export map", e);
			}
		}, Util.backgroundExecutor());

		return Command.SINGLE_SUCCESS;
	}

	private static CompletableFuture<Void> saveWorkspace(MinecraftServer server, MapWorkspace workspace) {
		return server.submit(() -> {
			ServerLevel workspaceWorld = server.getLevel(workspace.dimensionKey());
			workspaceWorld.save(null, true, false);
		});
	}

	private static MapWorkspace getCurrentWorkspace(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		MapWorkspaceManager workspaceManager = MapWorkspaceManager.get(source.getServer());

		MapWorkspace workspace = workspaceManager.getWorkspace(source.getLevel().dimension());
		if (workspace == null) {
			throw NOT_IN_WORKSPACE.create();
		}

		return workspace;
	}

	private static int importMap(CommandContext<CommandSourceStack> context, LevelStem dimension) throws CommandSyntaxException {
		ResourceLocation location = ResourceLocationArgument.getId(context, "location");
		String id = location.getPath();

		CommandSourceStack source = context.getSource();
		MinecraftServer server = source.getServer();
		MapWorkspaceManager workspaceManager = MapWorkspaceManager.get(server);

		if (workspaceManager.hasWorkspace(id)) {
			throw WORKSPACE_ALREADY_EXISTS.create(id);
		}

		if (!MapExportReader.exists(server, location)) {
			throw MAP_DOES_NOT_EXIST.create(location);
		}

		long seed = server.overworld().getSeed();
		WorkspaceDimensionConfig dimensionConfig = new WorkspaceDimensionConfig(dimension.type(), dimension.generator(), seed);

		workspaceManager.openWorkspace(id, dimensionConfig).thenAcceptAsync(workspace -> {
			try {
				try (MapExportReader reader = MapExportReader.open(server, location)) {
					MapMetadata metadata = reader.loadInto(server, workspace.dimensionKey());
					workspace.importFrom(metadata);

					source.sendSuccess(() -> Component.literal("Successfully imported workspace into '" + id + "'"), false);
				}
			} catch (IOException e) {
				source.sendFailure(Component.literal("Failed to import workspace!"));
				e.printStackTrace();
			}
		}, Util.backgroundExecutor());

		return Command.SINGLE_SUCCESS;
	}
}
