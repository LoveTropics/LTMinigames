package com.lovetropics.minigames.mixin.event;

import com.lovetropics.minigames.common.core.game.impl.GameEventDispatcher;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
	@Shadow
	public abstract ItemStack copy();

	@Inject(method = "useOn", at = @At("HEAD"))
	private void beforeUseOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		GameEventDispatcher.capturePlacedItemStack(copy());
	}

	@Inject(method = "useOn", at = @At("RETURN"))
	private void afterUseOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		GameEventDispatcher.clearPlacedItemStack();
	}
}
