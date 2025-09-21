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
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class FlamingPlantBehavior implements IGameBehavior {
	public static final MapCodec<FlamingPlantBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.fieldOf("radius").forGetter(b -> b.radius)
	).apply(i, FlamingPlantBehavior::new));
	private final int radius;

	private IGamePhase game;

	public FlamingPlantBehavior(int radius) {
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

		if (ticks % 15 != 0) {
			return;
		}

		ServerLevel world = game.level();

		Set<Mob> seen = new HashSet<>();

		for (Plant plant : plants) {
			AABB flameBounds = plant.coverage().asBounds().inflate(radius);
			List<Mob> entities = world.getEntitiesOfClass(Mob.class, flameBounds, BbMobEntity.PREDICATE);

			int max = 1 + random.nextInt(3);

			int count = random.nextInt(3);
			if (!entities.isEmpty()) {
				for (Mob entity : entities) {
					if (seen.size() > max) {
						break;
					}

					if (!seen.contains(entity)) {

						seen.add(entity);

						if (ignoreMob(entity)) {
							continue;
						}

						// In plant
						if (entity.blockPosition() == plant.coverage().getOrigin()) {
							entity.igniteForSeconds(6);

							if (random.nextInt(3) == 0) {
								entity.hurt(entity.damageSources().inFire(), 1 + random.nextInt(3));
							}
						} else {
							entity.igniteForSeconds(3);

							if (random.nextInt(3) == 0) {
								entity.hurt(entity.damageSources().inFire(), 1 + random.nextInt(2));
							}
						}

						AABB aabb = entity.getBoundingBox();

						Vec3 positionVec = entity.position();
						// Needs to target the middle of the entity position vector
						Vec3 scaledVec = new Vec3(positionVec.x, (aabb.minY + aabb.maxY) / 2.0, positionVec.z);

						Util.drawParticleBetween(ParticleTypes.FLAME, plant.coverage().asBounds().getCenter(), scaledVec, world, random, 10, 0.01, 0.02, 0.001, 0.01);
					}
				}
			}

			// Add more particles if attacked entities
			count += (!entities.isEmpty() ? 3 + random.nextInt(3) : 0);

			BlockPos pos = plant.coverage().getOrigin();
			if (random.nextInt(3) == 0) {

				for (int i = 0; i < count; ++i) {
					double d3 = random.nextGaussian() * 0.02;
					double d1 = random.nextGaussian() * 0.1;
					double d2 = random.nextGaussian() * 0.02;

					world.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1 + random.nextInt(2), d3, d1, d2, 0.002 + random.nextDouble() * random.nextDouble() * 0.025);
				}
			}
		}
	}

	private static boolean ignoreMob(Mob mob) {
		if (mob instanceof BbMobEntity bb) {
			return bb.immuneToFire();
		}

		return true;
	}
}
