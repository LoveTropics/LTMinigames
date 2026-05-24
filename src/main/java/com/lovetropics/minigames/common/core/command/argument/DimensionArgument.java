package com.lovetropics.minigames.common.core.command.argument;

import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.dimension.LevelStem;

public final class DimensionArgument {
	public static final DynamicCommandExceptionType DIMENSION_NOT_FOUND = new DynamicCommandExceptionType(arg ->
			Component.literal("Dimension does not exist with id: " + arg)
	);

	public static RequiredArgumentBuilder<CommandSourceStack, Identifier> argument(String name) {
		return Commands.argument(name, IdentifierArgument.id())
				.suggests((context, builder) -> {
					CommandSourceStack source = context.getSource();
					Registry<LevelStem> dimensions = source.getServer().registryAccess().lookupOrThrow(Registries.LEVEL_STEM);
					return SharedSuggestionProvider.suggestResource(
							dimensions.keySet().stream(),
							builder
					);
				});
	}

	public static LevelStem get(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
		Identifier key = IdentifierArgument.getId(context, name);

		CommandSourceStack source = context.getSource();
		Registry<LevelStem> dimensions = source.getServer().registryAccess().lookupOrThrow(Registries.LEVEL_STEM);

		LevelStem dimension = dimensions.getValue(key);
		if (dimension == null) {
			throw DIMENSION_NOT_FOUND.create(key);
		}

		return dimension;
	}
}
