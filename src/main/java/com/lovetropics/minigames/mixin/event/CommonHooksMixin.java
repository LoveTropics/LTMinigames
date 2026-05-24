package com.lovetropics.minigames.mixin.event;

import com.llamalad7.mixinextras.sugar.Local;
import com.lovetropics.minigames.common.core.game.impl.GameEventDispatcher;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.CommonHooks;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CommonHooks.class, remap = false)
public class CommonHooksMixin {
	// The Forge event is entirely not useful for our use-case, so let's hook in to the hook
	@Inject(method = "onPlayerTossEvent", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/event/entity/item/ItemTossEvent;<init>(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/entity/player/Player;)V"), cancellable = true, remap = false)
	private static void onPlayerToss(Player player, ItemStack item, boolean dropAround, boolean includeName, CallbackInfoReturnable<@Nullable ItemEntity> cir, @Local final ItemEntity entity) {
		// Only if the item actually originated from this player
		if (!includeName) {
			return;
		}
		if (player instanceof final ServerPlayer serverPlayer && GameEventDispatcher.instance.onPlayerThrowItem(serverPlayer, entity)) {
			cir.setReturnValue(null);
		}
	}
}
