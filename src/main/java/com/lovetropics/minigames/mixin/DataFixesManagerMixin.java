package com.lovetropics.minigames.mixin;

import net.minecraft.util.datafix.DataFixers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.concurrent.Executor;

@Mixin(DataFixers.class)
public class DataFixesManagerMixin {
	/**
	 * Makes DataFixerUpper lazy. Shh, don't tell.
	 */
	@ModifyArg(method = "createFixerUpper", at = @At(value = "INVOKE", target = "Lcom/mojang/datafixers/DataFixerBuilder;build(Ljava/util/concurrent/Executor;)Lcom/mojang/datafixers/DataFixer;", remap = false))
	private static Executor replaceFixerOptimizationExecutor(Executor executor) {
		return task -> {};
	}
}
