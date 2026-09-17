package com.lovetropics.minigames.mixin.client;

import com.lovetropics.minigames.client.game.ClientGameStateManager;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.lovetropics.minigames.common.core.game.client_state.instance.ForcePerspectiveClientState;
import net.minecraft.client.CameraType;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Options.class)
public class OptionsMixin {

	@Inject(method = "getCameraType", at = @At("HEAD"), cancellable = true)
	public void onGetCameraType(CallbackInfoReturnable<CameraType> cir) {
		ForcePerspectiveClientState forcePerspective = ClientGameStateManager.getOrNull(GameClientStateTypes.FORCE_PERSPECTIVE);
		if (forcePerspective != null) {
			switch (forcePerspective.perspective()) {
				case FIRST_PERSON -> cir.setReturnValue(CameraType.FIRST_PERSON);
				case THIRD_PERSON_BACK -> cir.setReturnValue(CameraType.THIRD_PERSON_BACK);
				case THIRD_PERSON_FRONT -> cir.setReturnValue(CameraType.THIRD_PERSON_FRONT);
			}
		}
	}
}
