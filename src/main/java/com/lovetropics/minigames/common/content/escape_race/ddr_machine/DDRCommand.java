package com.lovetropics.minigames.common.content.escape_race.ddr_machine;

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
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.JukeboxSong;
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

public final class DDRCommand {

	private static final SimpleCommandExceptionType NOT_DDRING = new SimpleCommandExceptionType(Component.literal("You are not playing DDR!"));
	private static final DynamicCommandExceptionType ERROR_INVALID_TRACK = new DynamicCommandExceptionType(
			p_399397_ -> Component.translatableEscape("Invalid track", p_399397_)
	);


	public static final SuggestionProvider<SharedSuggestionProvider> AVAILABLE_DISCS = SuggestionProviders.register(
			ResourceLocation.withDefaultNamespace("available_discs"),
			(context, suggestionsBuilder) ->
					SharedSuggestionProvider.suggestResource(context.getSource().registryAccess()
							.lookup(Registries.JUKEBOX_SONG)
							.get().keySet(), suggestionsBuilder)
	);

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		// @formatter:off
        dispatcher.register(
            literal("ddr")
				.requires(source -> source.hasPermission(2))
				.requires(CommandSourceStack::isPlayer)
					.then(literal("level")
							.then(literal("record")
									.then(literal("stop")
											.executes(DDRCommand::stopRecording))
									.then(argument("track", ResourceKeyArgument.key(Registries.JUKEBOX_SONG))
											.suggests(SuggestionProviders.cast(AVAILABLE_DISCS))
											.then(argument("name", StringArgumentType.string())
													.executes(DDRCommand::startRecording))))
					)
        );
        // @formatter:on
	}
	private static int startRecording(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ResourceKey<JukeboxSong> id = ResourceKeyArgument.getRegistryKey(context, "track", Registries.JUKEBOX_SONG, ERROR_INVALID_TRACK);
		String name = StringArgumentType.getString(context, "name");
		ServerPlayer player = context.getSource().getPlayer();
		if(player != null){
			if(player.getVehicle() != null && player.getVehicle() instanceof DDRMachineEntity ddrMachineEntity){
				ddrMachineEntity.startRecording(id, name);
				return Command.SINGLE_SUCCESS;
			}
		}
		throw NOT_DDRING.create();
	}

	private static int stopRecording(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayer();
		if(player != null){
			if(player.getVehicle() != null && player.getVehicle() instanceof DDRMachineEntity ddrMachineEntity){
				ddrMachineEntity.stopRecording();
				return Command.SINGLE_SUCCESS;
			}
		}
		throw NOT_DDRING.create();
	}

}
