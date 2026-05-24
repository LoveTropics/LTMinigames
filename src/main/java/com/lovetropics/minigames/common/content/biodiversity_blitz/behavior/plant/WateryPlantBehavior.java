package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant;

import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event.BbPlantEvents;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.BbMobEntity;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.Plant;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.util.Util;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class WateryPlantBehavior implements IGameBehavior {
	public static final MapCodec<WateryPlantBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.fieldOf("radius").forGetter(b -> b.radius)
	).apply(i, WateryPlantBehavior::new));
	private final int radius;

	private IGamePhase game;

	public WateryPlantBehavior(int radius) {
		this.radius = radius;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		this.game = game;
		events.listen(BbPlantEvents.TICK, this::tickPlants);
	}

	private void tickPlants(PlayerSet players, Plot plot, List<Plant> plants) {
		long ticks = game.ticks();
		RandomSource random = game.level().getRandom();

		if (ticks % 5 != 0) {
			return;
		}

		ServerLevel world = game.level();
		Set<Mob> seen = new HashSet<>();

		for (Plant plant : plants) {
			AABB attackBounds = plant.coverage().asBounds().inflate(radius);
			List<Mob> entities = world.getEntitiesOfClass(Mob.class, attackBounds, BbMobEntity.PREDICATE);

			if (entities.isEmpty()) {
				continue;
			}

			int max = 1 + random.nextInt(2);

			for (Mob entity : entities) {
				if (seen.size() > max) {
					break;
				}

				// Don't attack the same entity multiple times
				if (seen.contains(entity)) {
					continue;
				}

				seen.add(entity);

				int waterCount = 2 + random.nextInt(3);

				AABB aabb = entity.getBoundingBox();

				if (ticks % 20 == 0) {
					// Extinguish fire
					entity.setRemainingFireTicks(0);
					entity.hurt(entity.damageSources().magic(), 1 + random.nextInt(3));
					waterCount += 5 + random.nextInt(8);

					// Draw extra water as a line

					Vec3 positionVec = entity.position();
					// Needs to target the middle of the entity position vector
					Vec3 scaledVec = new Vec3(positionVec.x, (aabb.minY + aabb.maxY) / 2.0, positionVec.z);

					Util.drawParticleBetween(ParticleTypes.FALLING_WATER, plant.coverage().asBounds().getCenter(), scaledVec, world, random, 20, 0.05, 0.1, 0.03, 0.02);
				}

				// Don't add particles to mobs that should be dead
				if (entity.isDeadOrDying()) {
					continue;
				}

				for (int i = 0; i < waterCount; i++) {
					Vec3 sample = random(aabb, world.getRandom());
					double d3 = random.nextGaussian() * 0.05;
					double d1 = random.nextGaussian() * 0.1;
					double d2 = random.nextGaussian() * 0.05;
					world.sendParticles(ParticleTypes.FALLING_WATER, sample.x, sample.y, sample.z, 1 + random.nextInt(2), d3, d1, d2, 0.03 + random.nextDouble() * 0.02);
				}
			}
		}
	}

	private static Vec3 random(AABB aabb, RandomSource random) {
		return new Vec3(
				aabb.minX + random.nextDouble() * (aabb.maxX - aabb.minX),
				aabb.minY + random.nextDouble() * (aabb.maxY - aabb.minY),
				aabb.minZ + random.nextDouble() * (aabb.maxZ - aabb.minZ)
		);
	}
}
