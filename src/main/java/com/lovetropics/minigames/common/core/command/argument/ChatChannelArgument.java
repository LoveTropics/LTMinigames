package com.lovetropics.minigames.common.core.command.argument;

import com.lovetropics.minigames.common.core.chat.ChatChannel;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

public final class ChatChannelArgument {
	public static final DynamicCommandExceptionType CHANNEL_NOT_FOUND = new DynamicCommandExceptionType(arg ->
			Component.literal("Channel does not exist with id: " + arg)
	);

	public static RequiredArgumentBuilder<CommandSourceStack, String> argument(String name) {
		return Commands.argument(name, StringArgumentType.word())
				.suggests((context, builder) -> SharedSuggestionProvider.suggest(
						ChatChannel.names(),
						builder
				));
	}

	public static ChatChannel get(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
		String key = StringArgumentType.getString(context, name);
		ChatChannel channel = ChatChannel.CODEC.byName(key);
		if (channel == null) {
			throw CHANNEL_NOT_FOUND.create(key);
		}
		return channel;
	}
}
