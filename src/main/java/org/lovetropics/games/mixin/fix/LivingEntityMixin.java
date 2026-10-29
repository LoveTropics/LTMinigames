package org.lovetropics.games.mixin.fix;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
	@Inject(method = "maxUpStep", at = @At("RETURN"), cancellable = true)
	private void maxUpStep(CallbackInfoReturnable<Float> cir, @Local(name = "maxUpStep") float attributeValue) {
		// If the attribute is specifically overridden to 0, don't randomly make it larger
		if (attributeValue == 0.0f) {
			cir.setReturnValue(0.0f);
		}
	}
}
