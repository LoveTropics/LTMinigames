package org.lovetropics.games.common.content.escape_race;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.lovetropics.games.LoveTropics;

public final class EscapeRaceParticles {
	public static final DeferredRegister<ParticleType<?>> REGISTER = DeferredRegister.create(Registries.PARTICLE_TYPE, LoveTropics.ID);

	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BREAK_BUCK_PARTICLE = REGISTER.register("break_buck_particle", () -> new SimpleParticleType(false));
}
