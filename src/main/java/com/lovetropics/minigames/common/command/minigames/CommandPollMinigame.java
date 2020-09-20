package com.lovetropics.minigames.common.command.minigames;

import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;

import com.lovetropics.minigames.common.minigames.IMinigameDefinition;
import com.lovetropics.minigames.common.minigames.MinigameManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.command.CommandSource;
import net.minecraft.command.arguments.ResourceLocationArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;

public class CommandPollMinigame {
	public static void register(final CommandDispatcher<CommandSource> dispatcher) {
		dispatcher.register(
			literal("minigame")
			.then(literal("poll").requires(s -> s.hasPermissionLevel(2))
			.then(argument("minigame_id", ResourceLocationArgument.resourceLocation())
		              .suggests((ctx, sb) -> net.minecraft.command.ISuggestionProvider.suggest(
		                      MinigameManager.getInstance().getAllMinigames().stream()
		                          .map(IMinigameDefinition::getID)
		                          .map(net.minecraft.util.ResourceLocation::toString), sb))
		              .requires(s -> s.hasPermissionLevel(2))
			.executes(c -> {
				ResourceLocation id = ResourceLocationArgument.getResourceLocation(c, "minigame_id");
				int result = CommandMinigame.executeMinigameAction(() -> MinigameManager.getInstance().startPolling(id), c.getSource());

				if (result == 1 && c.getSource().getEntity() instanceof ServerPlayerEntity) {
					CommandMinigame.executeMinigameAction(() -> MinigameManager.getInstance().registerFor((ServerPlayerEntity) c.getSource().getEntity()), c.getSource());
				}

				return result;
		}))));
	}
}
