package com.lovetropics.minigames.mixin.gametest;

import com.lovetropics.minigames.common.util.LTGameTestFakePlayer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.gametest.framework.ReportGameListener")
public class GTReportGameListenerMixin {

	@Inject(at = @At("HEAD"), method = "lambda$say$0", cancellable = true)
	private static void dontSendToFakePlayers(ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
		if (player instanceof LTGameTestFakePlayer) {
			cir.setReturnValue(false);
		}
	}
}
