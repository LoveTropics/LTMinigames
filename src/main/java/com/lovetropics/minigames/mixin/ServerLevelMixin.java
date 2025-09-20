package com.lovetropics.minigames.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.lovetropics.minigames.common.core.game.impl.GameEventDispatcher;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {
	@Inject(method = "explode", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerExplosion;explode()V"))
	private void modifySound(
			@Nullable Entity entity,
			@Nullable DamageSource damageSource,
			@Nullable ExplosionDamageCalculator damageCalculator,
			double centerX,
			double centerY,
			double centerZ,
			float radius,
			boolean fire,
			Level.ExplosionInteraction interaction,
			ParticleOptions smallParticle,
			ParticleOptions bigParticle,
			Holder<SoundEvent> inputSound,
			CallbackInfo ci,
			@Local ServerExplosion explosion,
			@Local(argsOnly = true) LocalRef<Holder<SoundEvent>> sound
	) {
		sound.set(GameEventDispatcher.instance.modifyExplosionSound((ServerLevel) (Object) this, explosion, sound.get()));
	}
}
