package com.lovetropics.minigames.common.core.command.argument;

import com.lovetropics.minigames.common.core.map.workspace.MapWorkspace;
import com.lovetropics.minigames.common.core.map.workspace.MapWorkspaceManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

public final class MapWorkspaceArgument {
	private static final DynamicCommandExceptionType WORKSPACE_DOES_NOT_EXIST = new DynamicCommandExceptionType(id -> {
		return Component.literal("Workspace does not exist with id '" + id + "'");
	});

	public static RequiredArgumentBuilder<CommandSourceStack, String> argument(String name) {
		return Commands.argument(name, StringArgumentType.greedyString())
				.suggests((context, builder) -> {
					CommandSourceStack source = context.getSource();
					MapWorkspaceManager workspaceManager = MapWorkspaceManager.get(source.getServer());

					return SharedSuggestionProvider.suggest(
							workspaceManager.getWorkspaceIds().stream(),
							builder
					);
				});
	}

	public static MapWorkspace get(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		MapWorkspaceManager workspaceManager = MapWorkspaceManager.get(source.getServer());

		String id = StringArgumentType.getString(context, name);
		MapWorkspace workspace = workspaceManager.getWorkspace(id);
		if (workspace == null) {
			throw WORKSPACE_DOES_NOT_EXIST.create(id);
		}

		return workspace;
	}
}
