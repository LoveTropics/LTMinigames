package com.lovetropics.minigames.mixin;

import com.lovetropics.minigames.common.core.game.IGameManager;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.event.GameLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityType.class)
public class EntityTypeMixin<T extends Entity> {
	@Inject(
			method = "spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/EntitySpawnReason;ZZ)Lnet/minecraft/world/entity/Entity;",
			at = @At(value = "RETURN")
	)
	private void spawnEntity(ServerLevel level, ItemStack stack, LivingEntity source, BlockPos pos, EntitySpawnReason spawnType, boolean shouldOffsetY, boolean shouldOffsetYMore, CallbackInfoReturnable<T> ci) {
		T entity = ci.getReturnValue();
		if (entity instanceof LivingEntity livingEntity && source instanceof ServerPlayer serverPlayer) {
			IGamePhase game = IGameManager.get().getGamePhaseFor(entity);
			if (game != null) {
				game.invoker(GameLivingEntityEvents.SPAWNED).onSpawn(livingEntity, spawnType, serverPlayer);
			}
		}
	}
}
