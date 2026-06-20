package com.lovetropics.minigames.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.lovetropics.minigames.common.content.escape_race.client.ddr.render.DDRMachinePlayerHelper;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelFeatureRenderer.class)
public class ModelFeatureRendererMixin<S> {

	@Inject(method = "prepareModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/Model;setupAnim(Ljava/lang/Object;)V", shift = At.Shift.AFTER))
	public void onRenderModel(ModelFeatureRenderer.Submit<S> submit, CallbackInfo ci, @Local(name = "model") Model<S> model) {
		S state = submit.state();
		if (state instanceof LivingEntityRenderState renderState && model instanceof EntityModel<?> entityModel) {
			DDRMachinePlayerHelper.apply(renderState, entityModel);
		}
	}
}
