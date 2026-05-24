package com.lovetropics.minigames.common.core.command.argument;

import com.lovetropics.minigames.common.core.game.IGameDefinition;
import com.lovetropics.minigames.common.core.game.config.GameConfig;
import com.lovetropics.minigames.common.core.game.config.GameConfigs;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class GameConfigArgument {
	public static final DynamicCommandExceptionType GAME_CONFIG_NOT_FOUND = new DynamicCommandExceptionType(arg ->
			Component.literal("Game config does not exist with id: " + arg)
	);

	public static RequiredArgumentBuilder<CommandSourceStack, Identifier> argument(String name) {
		return Commands.argument(name, IdentifierArgument.id())
				.suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
						GameConfigs.REGISTRY.stream().map(IGameDefinition::id),
						builder
				));
	}

	public static GameConfig get(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
		Identifier id = IdentifierArgument.getId(context, name);

		GameConfig config = GameConfigs.REGISTRY.get(id);
		if (config == null) {
			throw GAME_CONFIG_NOT_FOUND.create(id);
		}

		return config;
	}
}
