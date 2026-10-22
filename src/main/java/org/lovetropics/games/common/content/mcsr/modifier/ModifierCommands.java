package org.lovetropics.games.common.content.mcsr.modifier;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.lovetropics.games.common.content.mcsr.McsrTexts;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.instances.command.CustomCommandBehavior;
import org.lovetropics.games.common.core.game.command.GameCommandRegistrar;

import java.util.Collection;
import java.util.Optional;

/// `/modifier` lets overlords change the level of the modifiers of the game, and choose who knows what they are
final class ModifierCommands {
	private static final DynamicCommandExceptionType UNKNOWN_MODIFIER = new DynamicCommandExceptionType(id -> McsrTexts.UNKNOWN_MODIFIER.apply(id));

	private ModifierCommands() {
	}

	static void register(IGamePhase game, GameModifiers modifiers, GameCommandRegistrar commands) {
		commands.register(Commands.literal("modifier")
				.requires(source -> CustomCommandBehavior.PermissionLevel.OVERLORD.hasPermission(game, source))
				.then(Commands.literal("list").executes(context -> list(context, modifiers)))
				.then(Commands.literal("set").then(modifierArgument(modifiers)
						.then(Commands.argument("level", IntegerArgumentType.integer(0))
								.executes(context -> setLevel(context, modifiers, IntegerArgumentType.getInteger(context, "level"))))))
				.then(Commands.literal("add").then(modifierArgument(modifiers)
						.then(Commands.argument("amount", IntegerArgumentType.integer())
								.executes(context -> {
									String id = getModifier(context, modifiers);
									return setLevel(context, modifiers, modifiers.level(id) + IntegerArgumentType.getInteger(context, "amount"));
								}))))
				.then(Commands.literal("reveal").then(Commands.argument("players", EntityArgument.players())
						.executes(context -> reveal(context, modifiers, Optional.empty()))
						.then(modifierArgument(modifiers)
								.executes(context -> reveal(context, modifiers, Optional.of(getModifier(context, modifiers)))))))
				.then(Commands.literal("hide").then(Commands.argument("players", EntityArgument.players())
						.then(modifierArgument(modifiers).executes(context -> hide(context, modifiers)))))
		);
	}

	private static RequiredArgumentBuilder<CommandSourceStack, String> modifierArgument(GameModifiers modifiers) {
		return Commands.argument("modifier", StringArgumentType.word())
				.suggests((context, builder) -> SharedSuggestionProvider.suggest(modifiers.ids(), builder));
	}

	private static String getModifier(CommandContext<CommandSourceStack> context, GameModifiers modifiers) throws CommandSyntaxException {
		String id = StringArgumentType.getString(context, "modifier");
		if (!modifiers.ids().contains(id)) {
			throw UNKNOWN_MODIFIER.create(id);
		}
		return id;
	}

	private static int list(CommandContext<CommandSourceStack> context, GameModifiers modifiers) {
		for (String id : modifiers.ids()) {
			context.getSource().sendSuccess(() -> McsrTexts.MODIFIER_LEVEL.apply(Component.literal(id).withStyle(ChatFormatting.AQUA), modifiers.level(id), modifiers.maxLevel(id)), false);
		}
		return modifiers.ids().size();
	}

	private static int setLevel(CommandContext<CommandSourceStack> context, GameModifiers modifiers, int level) throws CommandSyntaxException {
		String id = getModifier(context, modifiers);
		modifiers.setLevel(id, level);
		context.getSource().sendSuccess(() -> McsrTexts.MODIFIER_LEVEL.apply(Component.literal(id).withStyle(ChatFormatting.AQUA), modifiers.level(id), modifiers.maxLevel(id)), true);
		return modifiers.level(id);
	}

	private static int reveal(CommandContext<CommandSourceStack> context, GameModifiers modifiers, Optional<String> id) throws CommandSyntaxException {
		Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
		int count = 0;
		for (ServerPlayer player : players) {
			if (modifiers.reveal(player, id)) {
				count++;
			}
		}
		int revealed = count;
		context.getSource().sendSuccess(() -> McsrTexts.MODIFIER_REVEALED_TO.apply(revealed), true);
		return count;
	}

	private static int hide(CommandContext<CommandSourceStack> context, GameModifiers modifiers) throws CommandSyntaxException {
		String id = getModifier(context, modifiers);
		Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
		int count = 0;
		for (ServerPlayer player : players) {
			if (modifiers.hide(player, id)) {
				count++;
			}
		}
		int hidden = count;
		context.getSource().sendSuccess(() -> McsrTexts.MODIFIER_HIDDEN_FROM.apply(hidden), true);
		return count;
	}
}
