package com.lovetropics.minigames.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.lovetropics.minigames.client.game.ClientGameStateManager;
import com.lovetropics.minigames.common.core.data.LoveTropicsAttachments;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
	@WrapOperation(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getTeamColor()I"))
	private int modifyTeamColor(Entity entity, Operation<Integer> op) {
		if (entity.hasData(LoveTropicsAttachments.HIGHLIGHT_COLOR)) {
			return entity.getData(LoveTropicsAttachments.HIGHLIGHT_COLOR);
		}
		return op.call(entity);
	}

	@Inject(method = "collectVisibleEntities", at = @At("RETURN"), cancellable = true)
	private void collectVisibleEntities(Camera camera, Frustum frustum, List<Entity> output, CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(cir.getReturnValue() || ClientGameStateManager.getOrNull(GameClientStateTypes.HIGHLIGHT_BLOCKS) != null);
	}
}
