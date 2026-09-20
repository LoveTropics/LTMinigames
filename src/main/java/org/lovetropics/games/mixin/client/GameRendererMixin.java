package org.lovetropics.games.mixin.client;

import org.lovetropics.games.client.game.ClientGameStateManager;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

	@Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
	private void bobView(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
		if (ClientGameStateManager.getOrNull(GameClientStateTypes.DISABLE_BOBBING) != null) {
			ci.cancel();
		}
	}
}
