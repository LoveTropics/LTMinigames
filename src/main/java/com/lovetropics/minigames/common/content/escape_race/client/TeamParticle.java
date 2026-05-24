package com.lovetropics.minigames.common.content.escape_race.client;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.escape_race.EscapeRaceParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.HeartParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import org.jspecify.annotations.Nullable;

@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public class TeamParticle extends HeartParticle {
    TeamParticle(ClientLevel world, double x, double y, double z, SpriteSet sprites) {
        super(world, x, y, z, sprites.first());
    }

    @SubscribeEvent
    public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
		event.registerSpriteSet(EscapeRaceParticles.BREAK_BUCK_PARTICLE.get(), BreakBuckFactory::new);
    }

    public static class BreakBuckFactory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public BreakBuckFactory(SpriteSet pSprites) {
            sprites = pSprites;
        }

		@Override
		public @Nullable Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
			return new TeamParticle(level, x, y, z, sprites);
		}
    }

}
