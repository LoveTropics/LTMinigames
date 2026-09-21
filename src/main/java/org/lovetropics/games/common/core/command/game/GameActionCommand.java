package org.lovetropics.games.common.core.command.game;

import com.google.common.collect.Streams;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.core.game.IGameLookup;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorType;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorTypes;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.ActionSubjects;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.core.game.behavior.event.GameEventListeners;
import org.lovetropics.games.common.core.game.config.GameConfigs;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@EventBusSubscriber(modid = LoveTropics.ID)
public class GameActionCommand {
	private static final DynamicCommandExceptionType INVALID_ACTION = new DynamicCommandExceptionType(id -> Component.literal("No behavior with id: '" + id + "'"));
	private static final Dynamic2CommandExceptionType MALFORMED_ACTION_DATA = new Dynamic2CommandExceptionType((id, error) -> Component.literal("Malformed action data for '" + id + "': " + error));

	@SubscribeEvent
	public static void register(RegisterCommandsEvent event) {
		event.getDispatcher().register(literal("game")
				.then(literal("action").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(argument("id", IdentifierArgument.id()).suggests(GameActionCommand::suggestBehaviors)
								.then(argument("data", NbtTagArgument.nbtTag())
										.executes(ctx -> runAction(ctx, null))
										.then(argument("targets", EntityArgument.entities())
												.executes(ctx -> runAction(ctx, EntityArgument.getEntities(ctx, "targets"))))
								)
						)
				)
		);
	}

	private static CompletableFuture<Suggestions> suggestBehaviors(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
		return SharedSuggestionProvider.suggestResource(Streams.concat(
				GameBehaviorTypes.REGISTRY.keySet().stream(),
				GameConfigs.CUSTOM_BEHAVIORS.keySet().stream()
		), builder);
	}

	private static int runAction(CommandContext<CommandSourceStack> ctx, Collection<? extends Entity> targets) throws CommandSyntaxException {
		IGamePhase game = IGameLookup.get().getGamePhaseFor(ctx.getSource());
		if (game != null) {
			IGameBehavior behavior = parseBehavior(ctx);
			GameEventListeners events = new GameEventListeners();
			behavior.register(game, events);
			if (events.invoker(GameActionEvents.APPLY).apply(ContextMap.EMPTY, ActionSubjects.ofEntities(targets))) {
				ctx.getSource().sendSuccess(() -> Component.literal("Successfully applied action"), false);
			} else {
				ctx.getSource().sendFailure(Component.literal("No action was applied"));
			}
		}
		return Command.SINGLE_SUCCESS;
	}

	private static IGameBehavior parseBehavior(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		Identifier id = IdentifierArgument.getId(ctx, "id");
		Tag data = NbtTagArgument.getNbtTag(ctx, "data");
		GameBehaviorType<?> type = GameBehaviorTypes.REGISTRY.getValue(id);
		if (type == null) {
			type = GameConfigs.CUSTOM_BEHAVIORS.get(id);
		}
		if (type == null) {
			throw INVALID_ACTION.create(id);
		}

		RegistryOps<Tag> ops = ctx.getSource().registryAccess().createSerializationContext(NbtOps.INSTANCE);
		return type.codec().codec().parse(ops, data).getOrThrow(error -> MALFORMED_ACTION_DATA.create(id, error));
	}
}
