package com.lovetropics.minigames.mixin.fix;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.waypoints.ServerWaypointManager;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// TODO: Remove in 1.21.10+
@Mixin(ServerWaypointManager.class)
public class ServerwaypointManagerMixin {
	@Inject(method = "isLocatorBarEnabledFor", at = @At("HEAD"), cancellable = true)
	private static void locatorBarEnabledFor(ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(player.level().getGameRules().getBoolean(GameRules.RULE_LOCATOR_BAR));
	}
}
