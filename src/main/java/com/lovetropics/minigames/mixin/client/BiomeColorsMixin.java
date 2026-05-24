package com.lovetropics.minigames.mixin.client;

import com.lovetropics.minigames.client.game.ClientGameStateManager;
import com.lovetropics.minigames.common.content.biodiversity_blitz.BiodiversityBlitz;
import com.lovetropics.minigames.common.content.biodiversity_blitz.client_state.CheckeredPlotsState;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BiomeColors.class)
public class BiomeColorsMixin {
	@Inject(method = "getAverageGrassColor", at = @At("HEAD"), cancellable = true)
	private static void getGrassColor(BlockAndTintGetter world, BlockPos pos, CallbackInfoReturnable<Integer> ci) {
		CheckeredPlotsState checkeredPlots = ClientGameStateManager.getOrNull(BiodiversityBlitz.CHECKERED_PLOTS_STATE);
		if (checkeredPlots != null && checkeredPlots.contains(pos)) {
			boolean checkerboard = (pos.getX() + pos.getZ() & 1) == 0;
			ci.setReturnValue(checkerboard ? 0x4CCE35 : 0x2E9641);
		}
	}
}
