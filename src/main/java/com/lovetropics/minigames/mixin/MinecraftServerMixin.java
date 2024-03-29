package com.lovetropics.minigames.mixin;

import com.lovetropics.minigames.LoveTropics;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
	@Inject(method = "stopServer", at = @At("HEAD"))
	private void stopServer(CallbackInfo ci) {
		LoveTropics.onServerStoppingUnsafely((MinecraftServer) (Object) this);
	}
}
