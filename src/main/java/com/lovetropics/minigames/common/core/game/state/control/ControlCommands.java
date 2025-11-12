package com.lovetropics.minigames.common.core.game.state.control;

import com.lovetropics.minigames.common.core.game.state.GameStateKey;
import com.lovetropics.minigames.common.core.game.state.IGameState;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.commands.CommandSourceStack;

import java.util.Map;
import java.util.stream.Stream;

public final class ControlCommands implements IGameState {
	public static final GameStateKey.Defaulted<ControlCommands> KEY = GameStateKey.create("Control Commands", ControlCommands::new);

	private final Map<String, ControlCommand> commands = new Object2ObjectOpenHashMap<>();

	public void add(String name, ControlCommand command) {
		commands.put(name, command);
	}

	public void invoke(String name, CommandSourceStack source) throws CommandSyntaxException {
		ControlCommand command = commands.get(name);
		if (command != null) {
			command.invoke(source);
		}
	}

	public Stream<String> list(CommandSourceStack source) {
		return commands.entrySet().stream()
				.filter(entry -> entry.getValue().canUse(source))
				.map(Map.Entry::getKey);
	}
}
