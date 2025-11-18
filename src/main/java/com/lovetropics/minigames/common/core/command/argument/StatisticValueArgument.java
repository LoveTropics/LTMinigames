package com.lovetropics.minigames.common.core.command.argument;

import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.serialization.JavaOps;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;

public final class StatisticValueArgument {
	private static final DynamicCommandExceptionType FORMAT_ERROR = new DynamicCommandExceptionType(error ->
			Component.literal("Malformed statistic value: " + error)
	);

	private static final TagParser<Object> TAG_PARSER = TagParser.create(JavaOps.INSTANCE);

	public static RequiredArgumentBuilder<CommandSourceStack, String> argument(String name) {
		return Commands.argument(name, StringArgumentType.greedyString());
	}

	public static <T> T get(CommandContext<CommandSourceStack> context, String name, StatisticKey<T> statisticKey) throws CommandSyntaxException {
		String valueString = StringArgumentType.getString(context, name);
		Object valueTag = TAG_PARSER.parseAsArgument(new StringReader(valueString));
		return statisticKey.valueCodec().parse(JavaOps.INSTANCE, valueTag).getOrThrow(FORMAT_ERROR::create);
	}
}
