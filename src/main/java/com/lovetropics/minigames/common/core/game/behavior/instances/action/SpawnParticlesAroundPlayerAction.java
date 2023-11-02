package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.function.Function;

public record SpawnParticlesAroundPlayerAction(ParticleOptions[] particles, IntProvider count, IntProvider repeats, double radius, Vec3 offset, float speed, Optional<Vec3> position) implements IGameBehavior {
	public static final MapCodec<SpawnParticlesAroundPlayerAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			MoreCodecs.arrayOrUnit(ParticleTypes.CODEC, ParticleOptions[]::new).fieldOf("particles").forGetter(c -> c.particles),
			IntProvider.POSITIVE_CODEC.optionalFieldOf("count", ConstantInt.of(1)).forGetter(c -> c.count),
			IntProvider.POSITIVE_CODEC.optionalFieldOf("repeats", ConstantInt.of(1)).forGetter(c -> c.repeats),
			Codec.DOUBLE.optionalFieldOf("radius", 0.0).forGetter(c -> c.radius),
			Vec3.CODEC.optionalFieldOf("offset", new Vec3(0.1, 0.1, 0.1)).forGetter(SpawnParticlesAroundPlayerAction::offset),
			Codec.FLOAT.optionalFieldOf("speed", 0.0f).forGetter(SpawnParticlesAroundPlayerAction::speed),
			Vec3.CODEC.optionalFieldOf("position").forGetter(SpawnParticlesAroundPlayerAction::position)
	).apply(i, SpawnParticlesAroundPlayerAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		final RandomSource random = game.getRandom();
		final Function<ServerPlayer, Vec3> positionGenerator = createPositionGenerator(random);
		events.listen(GameActionEvents.APPLY_TO_PLAYER, (context, player) -> {
			int count = this.count.sample(random);
			for (int i = 0; i < count; i++) {
				ParticleOptions particle = particles[random.nextInt(particles.length)];
				int repeats = this.repeats.sample(random);
				Vec3 pos = positionGenerator.apply(player);
				game.getLevel().sendParticles(particle, pos.x, pos.y, pos.z, repeats, offset.x, offset.y, offset.z, speed);
			}
			return true;
		});
	}

	private Function<ServerPlayer, Vec3> createPositionGenerator(RandomSource random) {
		if (position.isPresent()) {
			return player -> {
				final double deltaX = random.triangle(-radius, radius);
				final double deltaY = random.triangle(-radius, radius);
				final double deltaZ = random.triangle(-radius, radius);
				return player.position().add(position.get()).add(deltaX, deltaY, deltaZ);
			};
		} else {
			return player -> {
				final AABB bounds = player.getBoundingBox().inflate(radius);
				return new Vec3(
						bounds.minX + random.nextDouble() * bounds.getXsize(),
						bounds.minY + random.nextDouble() * bounds.getYsize(),
						bounds.minZ + random.nextDouble() * bounds.getZsize()
				);
			};
		}
	}
}
