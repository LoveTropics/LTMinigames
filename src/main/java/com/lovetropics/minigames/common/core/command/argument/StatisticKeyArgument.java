package com.lovetropics.minigames.common.core.command.argument;

import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

public final class StatisticKeyArgument {
	public static final DynamicCommandExceptionType STATISTIC_NOT_FOUND = new DynamicCommandExceptionType(id ->
			Component.literal("Statistic does not exist with id: " + id)
	);

	public static RequiredArgumentBuilder<CommandSourceStack, String> argument(String name) {
		return Commands.argument(name, StringArgumentType.word())
				.suggests((context, builder) -> SharedSuggestionProvider.suggest(
						StatisticKey.keys().stream(),
						builder
				));
	}

	public static StatisticKey<?> get(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
		String id = StringArgumentType.getString(context, name);

		StatisticKey<?> key = StatisticKey.get(id);
		if (key == null) {
			throw STATISTIC_NOT_FOUND.create(id);
		}

		return key;
	}
}
