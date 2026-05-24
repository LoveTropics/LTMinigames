package com.lovetropics.minigames.common.core.command.game;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePackageEvents;
import com.lovetropics.minigames.common.core.game.impl.GamePhaseManager;
import com.lovetropics.minigames.common.core.game.state.GamePackageState;
import com.lovetropics.minigames.common.core.integration.game_actions.GamePackage;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class GamePackageCommand {
	public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
				literal("game")
						.then(literal("package").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.then(argument("id", StringArgumentType.word())
										.suggests(GamePackageCommand::suggestPackages)
										.executes(ctx -> GamePackageCommand.spawnPackage(ctx, null))
										.then(argument("target", EntityArgument.player())
												.executes(ctx -> GamePackageCommand.spawnPackage(ctx, EntityArgument.getPlayer(ctx, "target"))))))
		);
	}

	private static CompletableFuture<Suggestions> suggestPackages(final CommandContext<CommandSourceStack> ctx, final SuggestionsBuilder builder) {
		IGamePhase game = GamePhaseManager.get().getGamePhaseFor(ctx.getSource());
		if (game != null) {
			GamePackageState packages = game.state().get(GamePackageState.KEY);
			return SharedSuggestionProvider.suggest(packages.keys(), builder);
		}
		return Suggestions.empty();
	}

	private static int spawnPackage(CommandContext<CommandSourceStack> ctx, @Nullable ServerPlayer target) {
		IGamePhase game = GamePhaseManager.get().getGamePhaseFor(ctx.getSource());
		if (game != null) {
			String type = StringArgumentType.getString(ctx, "id");
			GamePackage gamePackage = new GamePackage(type, "LoveTropics", Optional.ofNullable(target).map(Entity::getUUID), Optional.empty());
			TriState result = game.invoker(GamePackageEvents.RECEIVE_PACKAGE).onReceivePackage(gamePackage);
			switch (result) {
				case TRUE ->
						ctx.getSource().sendSuccess(() -> Component.translatable("Successfully sent '%s'", type), true);
				case DEFAULT -> ctx.getSource().sendFailure(Component.translatable("'%s' was not processed", type));
				case FALSE -> ctx.getSource().sendFailure(Component.translatable("'%s' was rejected", type));
			}
		}
		return Command.SINGLE_SUCCESS;
	}
}
