package com.lovetropics.minigames.mixin.command;

import com.llamalad7.mixinextras.sugar.Local;
import com.lovetropics.minigames.common.core.game.command.GameCommandManager;
import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public class CommandsMixin {
	@Inject(method = "sendCommands", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/server/command/CommandHelper;mergeCommandNode(Lcom/mojang/brigadier/tree/CommandNode;Lcom/mojang/brigadier/tree/CommandNode;Ljava/util/Map;Ljava/lang/Object;Lcom/mojang/brigadier/Command;Ljava/util/function/Function;)V"))
	private void beforeSendCommands(ServerPlayer player, CallbackInfo ci, @Local RootCommandNode<CommandSourceStack> localTree) {
		GameCommandManager.addCommandsForClient(player, localTree);
	}
}
