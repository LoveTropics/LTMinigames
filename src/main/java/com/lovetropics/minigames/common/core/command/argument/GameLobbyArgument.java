package com.lovetropics.minigames.common.core.command.argument;

import com.lovetropics.minigames.common.core.game.impl.GameLobby;
import com.lovetropics.minigames.common.core.game.impl.GameLobbyManager;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public final class GameLobbyArgument {
	public static final DynamicCommandExceptionType GAME_LOBBY_NOT_FOUND = new DynamicCommandExceptionType(arg ->
			Component.literal("Could not find lobby with name: " + arg)
	);

	public static RequiredArgumentBuilder<CommandSourceStack, UUID> argument(String name) {
		return Commands.argument(name, UuidArgument.uuid())
				.suggests((context, builder) -> SharedSuggestionProvider.suggest(
						GameLobbyManager.get().getVisibleLobbies(context.getSource()).map(lobby -> lobby.getMetadata().id().uuid().toString()),
						builder
				));
	}

	public static GameLobby get(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
		UUID id = UuidArgument.getUuid(context, name);

		GameLobby lobby = GameLobbyManager.get().getLobbyById(id);
		if (lobby == null || !lobby.isVisibleTo(context.getSource())) {
			throw GAME_LOBBY_NOT_FOUND.create(id);
		}

		return lobby;
	}
}
