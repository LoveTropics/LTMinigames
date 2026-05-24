package com.lovetropics.minigames.mixin.event;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.lovetropics.minigames.common.core.game.impl.GameEventDispatcher;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerExplosion;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {

	@Inject(method = "explode", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerExplosion;explode()I"))
	private void modifySound(
			@Nullable Entity source,
			@Nullable DamageSource damageSource,
			@Nullable ExplosionDamageCalculator damageCalculator,
			double x,
			double y,
			double z,
			float r,
			boolean fire,
			Level.ExplosionInteraction interactionType,
			ParticleOptions smallExplosionParticles,
			ParticleOptions largeExplosionParticles,
			WeightedList<ExplosionParticleInfo> blockParticles,
			Holder<SoundEvent> explosionSound,
			CallbackInfo ci,
			@Local(name = "explosion") ServerExplosion explosion,
			@Local(argsOnly = true, name = "explosionSound") LocalRef<Holder<SoundEvent>> sound) {
		sound.set(GameEventDispatcher.instance.modifyExplosionSound((ServerLevel) (Object) this, explosion, sound.get()));
	}
}
