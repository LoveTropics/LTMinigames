package com.lovetropics.minigames.mixin.fix;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.lovetropics.minigames.common.core.dimension.RuntimeDimensions;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.GameRuleCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRuleCommand.class)
public class GameRuleCommandMixin {
	@WrapOperation(method = "setRule", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getGameRules()Lnet/minecraft/world/level/GameRules;"))
	private static GameRules getGameRules(MinecraftServer server, Operation<GameRules> original, CommandContext<CommandSourceStack> context) {
		ServerLevel level = context.getSource().getLevel();
		return RuntimeDimensions.isTemporaryDimension(level) ? level.getGameRules() : original.call(server);
	}
}
