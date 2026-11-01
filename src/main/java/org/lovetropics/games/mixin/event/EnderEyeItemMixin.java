package org.lovetropics.games.mixin.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.EnderEyeItem;
import net.minecraft.world.item.context.UseOnContext;
import org.lovetropics.games.common.core.game.impl.GameEventDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderEyeItem.class)
public class EnderEyeItemMixin {
	@Inject(
			method = "useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/pattern/BlockPattern$BlockPatternMatch;getFrontTopLeft()Lnet/minecraft/core/BlockPos;"),
			cancellable = true
	)
	public void useOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		if(!(context.getLevel() instanceof ServerLevel serverLevel)) return;
		if(GameEventDispatcher.instance.onEndPortalSpawn(serverLevel, context.getClickedPos())){
			cir.setReturnValue(InteractionResult.SUCCESS);
		}
	}
}
