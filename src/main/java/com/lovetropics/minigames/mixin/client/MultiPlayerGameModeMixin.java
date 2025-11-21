package com.lovetropics.minigames.mixin.client;

import com.lovetropics.minigames.client.game.ClientGameStateManager;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
	@Shadow
	private int destroyDelay;

	@Inject(method = "startDestroyBlock", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/common/CommonHooks;onLeftClickBlock(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket$Action;)Lnet/neoforged/neoforge/event/entity/player/PlayerInteractEvent$LeftClickBlock;"))
	private void startDestroyBlock(BlockPos loc, Direction face, CallbackInfoReturnable<Boolean> cir) {
		if (ClientGameStateManager.getOrNull(GameClientStateTypes.BREAK_DELAY) != null) {
			destroyDelay = Math.max(5, destroyDelay);
		}
	}
}
