package com.lovetropics.minigames.mixin.disguise;

import com.lovetropics.minigames.common.core.diguise.PlayerDisguise;
import com.lovetropics.minigames.common.core.diguise.PlayerDisguiseBehavior;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
	@Shadow
	@Final
	public WalkAnimationState walkAnimation;

	private LivingEntityMixin(EntityType<?> type, Level level) {
		super(type, level);
	}

	@Inject(method = "calculateEntityAnimation", at = @At("HEAD"), cancellable = true)
	private void calculateEntityAnimation(final boolean includeHeight, final CallbackInfo ci) {
		final PlayerDisguise disguise = PlayerDisguise.getOrNull(this);
		if (disguise != null && disguise.entity() instanceof final LivingEntity disguiseEntity) {
			disguiseEntity.calculateEntityAnimation(includeHeight);
			PlayerDisguiseBehavior.copyWalkAnimation(disguiseEntity.walkAnimation, walkAnimation);
			ci.cancel();
		}
	}
}
