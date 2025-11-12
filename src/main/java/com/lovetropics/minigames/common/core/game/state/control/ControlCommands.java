package com.lovetropics.minigames.common.core.game.state.control;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.commands.CommandSourceStack;

import java.util.Map;
import java.util.stream.Stream;

public final class ControlCommands implements ControlCommandInvoker, ControlCommandRegistrar {
	private final Map<String, ControlCommand> commands = new Object2ObjectOpenHashMap<>();

	@Override
	public void register(String name, ControlCommand.Scope scope, ControlCommand.Action action) {
		commands.put(name, new ControlCommand(scope, action));
	}

	@Override
	public void invoke(String name, CommandSourceStack source) throws CommandSyntaxException {
		ControlCommand command = commands.get(name);
		if (command != null) {
			command.invoke(source);
		}
	}

	@Override
	public Stream<String> list(CommandSourceStack source) {
		return commands.entrySet().stream()
				.filter(entry -> entry.getValue().scope().canUse(source))
				.map(Map.Entry::getKey);
	}
}
