package org.lovetropics.games.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;
import org.lovetropics.games.client.game.ClientGameStateManager;
import org.lovetropics.games.common.core.game.client_state.GameClientStateSender;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import org.lovetropics.games.common.util.registry.GameClientTweakEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TridentItem.class)
public class TridentItemMixin {
	@Inject(method = "releaseUsing", at = @At("HEAD"), cancellable = true)
	private void releaseUsing(ItemStack itemStack, Level level, LivingEntity entity, int remainingTime, CallbackInfoReturnable<Boolean> cir) {
		if (lt$isSetOnClientOrSet(entity, GameClientStateTypes.DISABLE_PLAYER_MOVEMENT)) {
			cir.setReturnValue(false);
		}
	}

	@WrapOperation(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;push(DDD)V"))
	private void pushRiptide(Player player, double dx, double dy, double dz, Operation<Void> original) {
		// TODO: This is so unbelievably stupid, I am sorry
		boolean reversed = lt$isSetOnClientOrSet(player, GameClientStateTypes.REVERSE_RIPTIDE);
		if (reversed) {
			original.call(player, -dx, -dy, -dz);
		} else {
			original.call(player, dx, dy, dz);
		}
	}

	@Unique
	private static boolean lt$isSetOnClientOrSet(Entity player, GameClientTweakEntry<?> type) {
		if (player instanceof ServerPlayer serverPlayer) {
			return GameClientStateSender.get().byPlayer(serverPlayer).getOrNull(type.get()) != null;
		} else {
			return ClientGameStateManager.isSet(type);
		}
	}
}
