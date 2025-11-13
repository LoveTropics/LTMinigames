package com.lovetropics.minigames.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.lovetropics.minigames.common.core.data.LoveTropicsAttachments;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
	@WrapOperation(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getTeamColor()I"))
	private int modifyTeamColor(Entity entity, Operation<Integer> op) {
		if (entity.hasData(LoveTropicsAttachments.HIGHLIGHT_COLOR)) {
			return entity.getData(LoveTropicsAttachments.HIGHLIGHT_COLOR);
		}
		return op.call(entity);
	}
}
