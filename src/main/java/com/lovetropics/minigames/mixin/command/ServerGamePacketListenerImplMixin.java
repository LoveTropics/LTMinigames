package com.lovetropics.minigames.mixin.command;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.lovetropics.minigames.common.core.game.command.GameCommandManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
	@WrapOperation(method = "handleCustomCommandSuggestions", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/CommandDispatcher;getCompletionSuggestions(Lcom/mojang/brigadier/ParseResults;)Ljava/util/concurrent/CompletableFuture;"))
	private CompletableFuture<Suggestions> handleCommandSuggestions(CommandDispatcher<CommandSourceStack> dispatcher, ParseResults<CommandSourceStack> parse, Operation<CompletableFuture<Suggestions>> original) {
		CompletableFuture<Suggestions> baseFuture = original.call(dispatcher, parse);
		CompletableFuture<Suggestions> gameFuture = GameCommandManager.getCommandSuggestions(parse);
		if (gameFuture != null) {
			return baseFuture.thenCombine(gameFuture, (baseSuggestions, gameSuggestions) ->
					Suggestions.merge(parse.getReader().getString(), List.of(baseSuggestions, gameSuggestions))
			);
		}
		return baseFuture;
	}
}
