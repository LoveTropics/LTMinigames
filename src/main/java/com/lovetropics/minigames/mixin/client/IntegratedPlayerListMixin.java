package com.lovetropics.minigames.mixin.client;

import com.lovetropics.minigames.common.core.game.PlayerIsolation;
import net.minecraft.client.server.IntegratedPlayerList;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IntegratedPlayerList.class)
public class IntegratedPlayerListMixin {
	// Todo 26.1 Port
//	@Inject(method = "save", at = @At("HEAD"), cancellable = true)
//	private void save(final ServerPlayer player, final CallbackInfo ci) {
//		if (PlayerIsolation.INSTANCE.isIsolated(player)) {
//			ci.cancel();
//		}
//	}
}
