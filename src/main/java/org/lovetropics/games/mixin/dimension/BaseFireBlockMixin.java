package org.lovetropics.games.mixin.dimension;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.lovetropics.games.common.core.dimension.LinkedDimensions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BaseFireBlock.class)
public class BaseFireBlockMixin {
	@WrapOperation(method = "inPortalDimension", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;dimension()Lnet/minecraft/resources/ResourceKey;"))
	private static ResourceKey<Level> getVanillaDimension(Level level, Operation<ResourceKey<Level>> original) {
		return LinkedDimensions.vanillaKey(level);
	}
}
