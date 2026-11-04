package org.lovetropics.games.mixin.fix;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/// [MC-221585](https://mojira.dev/MC-221585)
@Mixin(ExplosionDamageCalculator.class)
public class ExplosionDamageCalculatorMixin {
	@Inject(method = "getBlockExplosionResistance", at = @At("HEAD"), cancellable = true)
	private void getBlockExplosionResistance(Explosion explosion, BlockGetter level, BlockPos pos, BlockState block, FluidState fluid, CallbackInfoReturnable<Optional<Float>> cir) {
		if (block.is(Blocks.LIGHT)) {
			cir.setReturnValue(Optional.empty());
		}
	}

	@Inject(method = "shouldBlockExplode", at = @At("HEAD"), cancellable = true)
	private void shouldBlockExplode(Explosion explosion, BlockGetter level, BlockPos pos, BlockState state, float power, CallbackInfoReturnable<Boolean> cir) {
		if (state.is(Blocks.LIGHT)) {
			cir.setReturnValue(false);
		}
	}
}
