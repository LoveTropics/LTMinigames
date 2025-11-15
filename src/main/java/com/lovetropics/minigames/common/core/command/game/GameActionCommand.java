package com.lovetropics.minigames.common.core.command.game;

import com.google.common.collect.Streams;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GameEventListeners;
import com.lovetropics.minigames.common.core.game.config.GameConfigs;
import com.lovetropics.minigames.common.core.game.impl.GamePhaseManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.Entity;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class GameActionCommand {
	private static final DynamicCommandExceptionType INVALID_ACTION = new DynamicCommandExceptionType(id -> Component.literal("No behavior with id: '" + id + "'"));
	private static final Dynamic2CommandExceptionType MALFORMED_ACTION_DATA = new Dynamic2CommandExceptionType((id, error) -> Component.literal("Malformed action data for '" + id + "': " + error));

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(literal("game")
				.then(literal("action").requires(s -> s.hasPermission(2))
						.then(argument("id", ResourceLocationArgument.id()).suggests(GameActionCommand::suggestBehaviors)
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
		IGamePhase game = GamePhaseManager.get().getGamePhaseFor(ctx.getSource());
		if (game != null) {
			IGameBehavior behavior = parseBehavior(ctx);
			GameEventListeners events = new GameEventListeners();
			behavior.register(game, events);
			boolean result = false;
			for (var target : targets) {
				if (target instanceof ServerPlayer serverPlayer) {
					result |= events.invoker(GameActionEvents.APPLY_TO_PLAYER).apply(ContextMap.EMPTY, serverPlayer);
				} else if (target != null) {
					result |= events.invoker(GameActionEvents.APPLY_TO_ENTITY).apply(ContextMap.EMPTY, target);
				} else {
					result |= events.invoker(GameActionEvents.APPLY).apply(ContextMap.EMPTY);
				}
			}
			if (result) {
				ctx.getSource().sendSuccess(() -> Component.literal("Successfully applied action"), false);
			} else {
				ctx.getSource().sendFailure(Component.literal("No action was applied"));
			}
		}
		return Command.SINGLE_SUCCESS;
	}

	private static IGameBehavior parseBehavior(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
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
