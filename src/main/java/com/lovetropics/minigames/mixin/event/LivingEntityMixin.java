package com.lovetropics.minigames.mixin.event;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.lovetropics.minigames.client.game.ClientGameStateManager;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
	@WrapOperation(method = "checkAutoSpinAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
	private List<Entity> filterCollidedEntities(Level level, Entity entity, AABB aabb, Operation<List<Entity>> original) {
		if (level.isClientSide() && ClientGameStateManager.getOrNull(GameClientStateTypes.DISABLE_PLAYER_COLLISION) != null) {
			return List.of();
		}
		return original.call(level, entity, aabb);
	}
}
