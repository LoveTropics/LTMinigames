package com.lovetropics.minigames.common.core.command.game;

import com.lovetropics.minigames.common.core.game.IActiveGame;
import com.lovetropics.minigames.common.core.game.IGameManager;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePackageEvents;
import com.lovetropics.minigames.common.core.game.behavior.instances.donation.DonationPackageBehavior;
import com.lovetropics.minigames.common.core.integration.game_actions.GamePackage;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;

public class GamePackageCommand {
	public static void register(final CommandDispatcher<CommandSource> dispatcher) {
		dispatcher.register(
			literal("game")
			.then(literal("package").requires(s -> s.hasPermissionLevel(2))
				.then(argument("id", StringArgumentType.word())
					.suggests(GamePackageCommand::suggestPackages)
						.executes(ctx -> GamePackageCommand.spawnPackage(ctx, null))
						.then(argument("target", EntityArgument.player())
							.executes(ctx -> GamePackageCommand.spawnPackage(ctx, EntityArgument.getPlayer(ctx, "target"))))))
		);
	}

	private static CompletableFuture<Suggestions> suggestPackages(final CommandContext<CommandSource> context, final SuggestionsBuilder builder) {
		IActiveGame game = IGameManager.get().getActiveGameFor(context.getSource());
		if (game != null) {
			return ISuggestionProvider.suggest(game.getBehaviors().stream()
					.filter(b -> b instanceof DonationPackageBehavior)
					.map(b -> ((DonationPackageBehavior)b).getPackageType()), builder);
		}
		return Suggestions.empty();
	}

	private static int spawnPackage(CommandContext<CommandSource> ctx, ServerPlayerEntity target) {
		IActiveGame game = IGameManager.get().getActiveGameFor(ctx.getSource());
		if (game != null) {
			String type = StringArgumentType.getString(ctx, "id");
			GamePackage gamePackage = new GamePackage(type, "LoveTropics", target == null ? null : target.getUniqueID());
			game.invoker(GamePackageEvents.RECEIVE_PACKAGE).onReceivePackage(game, gamePackage);
		}
		return Command.SINGLE_SUCCESS;
	}
}
