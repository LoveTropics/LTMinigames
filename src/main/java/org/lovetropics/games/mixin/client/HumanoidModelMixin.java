package org.lovetropics.games.mixin.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.lovetropics.games.common.content.escape_race.client.ddr.render.DDRMachinePlayerHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public class HumanoidModelMixin<S extends HumanoidRenderState> {
	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("RETURN"))
	public void onRenderModel(S state, CallbackInfo ci) {
		DDRMachinePlayerHelper.apply(state, (HumanoidModel<S>) (Object) this);
	}
}
