package com.lovetropics.minigames.common.content.escape_race.effect;

import com.lovetropics.minigames.SoundRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.CommonColors;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class UpsetStomachEffect extends MobEffect {
	public UpsetStomachEffect(MobEffectCategory category) {
		super(category, CommonColors.GREEN);
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
		if (level.getRandom().nextFloat() < 0.05f && entity.getKnownMovement().lengthSqr() > 0.001f) {
			level.playSound(null, entity.blockPosition(), SoundRegistry.UPSET_STOMACH_FART.value(), SoundSource.PLAYERS, 1f, level.getRandom().triangle(0.4f, 1.4f));
			level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, entity.position().x, entity.position().y + 0.5f, entity.position().z, level.getRandom().nextInt(3) + 1, 0, 0, 0, 0.1f);
			entity.hurtMarked = true;
			final float uppies = entity.onGround() && entity.isCrouching() ? 0.5f : 0.4f;
			entity.addDeltaMovement(entity.getKnownMovement().normalize().scale(0.2f).add(0, uppies, 0));
		}

		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}
}
