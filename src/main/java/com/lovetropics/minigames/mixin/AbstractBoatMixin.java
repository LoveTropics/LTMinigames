package com.lovetropics.minigames.mixin;

import com.lovetropics.minigames.client.game.ClientGameStateManager;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateSender;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractBoat;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(AbstractBoat.class)
public abstract class AbstractBoatMixin extends Entity {
	public AbstractBoatMixin(EntityType<?> entityType, Level level) {
		super(entityType, level);
	}

	@Inject(method = "canBeCollidedWith", at = @At("HEAD"), cancellable = true)
	private void canBeCollidedWith(Entity entity, CallbackInfoReturnable<Boolean> ci) {
		if (lt$shouldDisableCollision()) {
			ci.setReturnValue(false);
		}
	}

	@Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
	private void isPushable(CallbackInfoReturnable<Boolean> ci) {
		if (lt$shouldDisableCollision()) {
			ci.setReturnValue(false);
		}
	}

	@Unique
	private boolean lt$shouldDisableCollision() {
		if (getControllingPassenger() instanceof Player player) {
			if (player instanceof ServerPlayer serverPlayer) {
				return GameClientStateSender.getOrNull(serverPlayer, GameClientStateTypes.DISABLE_PLAYER_COLLISION.get()) != null;
			} else if (player.level().isClientSide()) {
				return ClientGameStateManager.getOrNull(GameClientStateTypes.DISABLE_PLAYER_COLLISION) != null;
			}
		}
		return false;
	}

	@Override
	@Shadow
	@Nullable
	public abstract LivingEntity getControllingPassenger();
}
