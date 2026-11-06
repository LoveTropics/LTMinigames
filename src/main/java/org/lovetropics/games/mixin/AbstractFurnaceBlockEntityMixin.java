package org.lovetropics.games.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.lovetropics.games.common.core.game.impl.GameEventDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceBlockEntity.class)
public  class AbstractFurnaceBlockEntityMixin  {

	@Inject(method = "getTotalCookTime", at = @At("TAIL"), cancellable = true)
	private static void getTotalCookTime(ServerLevel level, AbstractFurnaceBlockEntity entity, CallbackInfoReturnable<Integer> cir, @Local(name = "input")SingleRecipeInput input) {
		cir.setReturnValue(GameEventDispatcher.instance.onCookTimeResolved(level, cir.getReturnValue(), input.item()));
	}
}
