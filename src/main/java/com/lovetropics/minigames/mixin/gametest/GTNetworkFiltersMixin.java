package com.lovetropics.minigames.mixin.gametest;

import net.minecraft.network.Connection;
import net.neoforged.neoforge.network.filters.NetworkFilters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkFilters.class)
public class GTNetworkFiltersMixin {
	@Inject(at = @At("HEAD"), method = "injectIfNecessary", cancellable = true, remap = false)
	private static void dontInjectIfNotPossible(Connection manager, CallbackInfo ci) {
		if (manager.channel() == null) {
			ci.cancel();
		}
	}
}
