package com.lovetropics.minigames.common.core.command;


import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateType;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DataResult;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class ClientStateCommand {
	public static void register(CommandBuildContext buildContext, CommandDispatcher<CommandSourceStack> dispatcher) {
		ResourceArgument<GameClientStateType<?>> clientStateArgument = ResourceArgument.resource(buildContext, GameClientStateTypes.REGISTRY_KEY);

		dispatcher.register(literal("clientstate")
				.then(literal("set")
						.then(argument("state", clientStateArgument)
								.then(argument("data", CompoundTagArgument.compoundTag())
										.executes(ClientStateCommand::set)
								)
						)
				)
				.then(literal("reset")
						.then(argument("state", clientStateArgument)
								.executes(ClientStateCommand::reset)
						)
				)
		);
	}

	private static int set(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		CompoundTag data = CompoundTagArgument.getCompoundTag(context, "data");
		Holder.Reference<GameClientStateType<? extends GameClientState>> state = getState(context);

		DataResult<? extends GameClientState> decode = state.value().codec().compressedDecode(NbtOps.INSTANCE, data);
		GameClientState clientState = decode.getOrThrow();
		GameClientState.sendToPlayer(clientState, player);
		context.getSource().sendSuccess(() -> Component.literal("Updated Client State " + state.key().identifier()), false);
		return 1;
	}

	private static int reset(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		Holder.Reference<GameClientStateType<? extends GameClientState>> state = getState(context);

		GameClientState.removeFromPlayer(state.value(), player);
		context.getSource().sendSuccess(() -> Component.literal("Reset Client State " + state.key().identifier()), false);
		return Command.SINGLE_SUCCESS;
	}

	private static Holder.Reference<GameClientStateType<? extends GameClientState>> getState(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return ResourceArgument.getResource(context, "state", GameClientStateTypes.REGISTRY_KEY);
	}

}
