package net.tropicraft.lovetropics.common.command.minigames;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.tropicraft.lovetropics.common.minigames.MinigameManager;

import static net.minecraft.command.Commands.literal;

public class CommandStopMinigame {
	public static void register(final CommandDispatcher<CommandSource> dispatcher) {
		dispatcher.register(
			literal("minigame")
			.then(literal("stop").requires(s -> s.hasPermissionLevel(2))
			.executes(c -> CommandMinigame.executeMinigameAction(() ->
				MinigameManager.getInstance().stop(), c.getSource())))
		);
	}
}
