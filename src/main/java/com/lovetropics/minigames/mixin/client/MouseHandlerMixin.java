package com.lovetropics.minigames.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.lovetropics.minigames.client.game.ClientGameStateManager;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
	private void respectControlModificationState(LocalPlayer player, double dx, double dy, Operation<Void> original) {
		var state = ClientGameStateManager.getOrNull(GameClientStateTypes.INVERT_CONTROLS);
		if (state != null) {
			if (state.xAxis()) {
				dx = dx * -1;
			}

			// Avoid allowing the bypass of the setting by modifying the options
			if (state.yAxis() && !minecraft.options.invertMouseY().get()) {
				dy = dy * -1;
			}
		}

		original.call(player, dx, dy);
	}
}
