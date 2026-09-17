package com.lovetropics.minigames.mixin.dimension;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.lovetropics.minigames.common.core.dimension.LinkedDimensions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NetherPortalBlock;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(NetherPortalBlock.class)
public class NetherPortalBlockMixin {
	@WrapOperation(method = "getPortalDestination", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;dimension()Lnet/minecraft/resources/ResourceKey;"))
	private ResourceKey<Level> getVanillaDimension(ServerLevel level, Operation<ResourceKey<Level>> original) {
		return LinkedDimensions.vanillaKey(level);
	}

	@WrapOperation(method = "getPortalDestination", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getLevel(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/server/level/ServerLevel;"))
	private @Nullable ServerLevel getLinkedLevel(MinecraftServer server, ResourceKey<Level> dimension, Operation<ServerLevel> original, @Local(argsOnly = true) ServerLevel currentLevel) {
		ResourceKey<Level> linkedDimension = LinkedDimensions.resolve(currentLevel, dimension);
		return linkedDimension != null ? original.call(server, linkedDimension) : null;
	}
}
