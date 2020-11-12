package com.lovetropics.minigames.common.minigames.behaviours.instances.donations;

import com.mojang.datafixers.Dynamic;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;

import java.util.List;
import java.util.Optional;

public class EffectPackageBehavior extends DonationPackageBehavior
{
	private final List<StatusEffect> effects;

	public EffectPackageBehavior(final DonationPackageData data, final List<StatusEffect> effects) {
		super(data);

		this.effects = effects;
	}

	public static <T> EffectPackageBehavior parse(Dynamic<T> root) {
		final DonationPackageData data = DonationPackageData.parse(root);
		final List<StatusEffect> effects = root.get("effects").asList(StatusEffect::parse);

		return new EffectPackageBehavior(data, effects);
	}

	@Override
	protected void receivePackage(final String sendingPlayer, final ServerPlayerEntity player) {
		effects.forEach(effect -> effect.applyToPlayer(player));
	}

	public static class StatusEffect {
		private final ResourceLocation type;
		private final int seconds;
		private final int amplifier;
		private final boolean hideParticles;

		public StatusEffect(final ResourceLocation type, final int seconds, final int amplifier, final boolean hideParticles) {
			this.type = type;
			this.seconds = seconds;
			this.amplifier = amplifier;
			this.hideParticles = hideParticles;
		}

		public ResourceLocation getType() {
			return type;
		}

		public int getSeconds()
		{
			return seconds;
		}

		public int getAmplifier()
		{
			return amplifier;
		}

		public boolean hideParticles()
		{
			return hideParticles;
		}

		public Optional<Effect> getEffect() {
			return Registry.EFFECTS.getValue(type);
		}

		public void applyToPlayer(final ServerPlayerEntity player) {
			final Optional<Effect> effect = getEffect();

			effect.ifPresent(value -> player.addPotionEffect(new EffectInstance(value, seconds * 20, amplifier, false, !hideParticles)));
		}

		public static <T> StatusEffect parse(Dynamic<T> root) {
			final ResourceLocation type = new ResourceLocation(root.get("type").asString(""));
			final int seconds = root.get("seconds").asInt(0);
			final int amplifier = root.get("amplifier").asInt(0);
			final boolean hideParticles = root.get("hide_particles").asBoolean(false);

			return new StatusEffect(type, seconds, amplifier, hideParticles);
		}
	}
}
