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
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class LightningPlantBehavior implements IGameBehavior {
	public static final MapCodec<LightningPlantBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.fieldOf("radius").forGetter(b -> b.radius)
	).apply(i, LightningPlantBehavior::new));

	private static final int INTERVAL_TICKS = SharedConstants.TICKS_PER_SECOND * 8;

	private final int radius;

	private IGamePhase game;

	public LightningPlantBehavior(int radius) {
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

		if (ticks % INTERVAL_TICKS != 0) {
			return;
		}

		ServerLevel world = game.level();

		for (Plant plant : plants) {
			AABB flameBounds = plant.coverage().asBounds().inflate(radius);
			List<Mob> entities = world.getEntitiesOfClass(Mob.class, flameBounds, BbMobEntity.PREDICATE);

			// Select random entity and spawn lightning on top of it.
			if (!entities.isEmpty()) {
				Mob target = entities.get(random.nextInt(entities.size()));

				BlockPos pos = target.blockPosition();
				// TODO: custom lightning bolt class to prevent too loud sounds and fire!
				LightningBolt lightningbolt = EntityTypes.LIGHTNING_BOLT.create(world, EntitySpawnReason.COMMAND);
				lightningbolt.snapTo(Vec3.atBottomCenterOf(pos));
				lightningbolt.setCause(null);
				world.addFreshEntity(lightningbolt);
			}
		}
	}
}
