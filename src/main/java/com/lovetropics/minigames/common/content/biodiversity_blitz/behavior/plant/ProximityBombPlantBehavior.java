package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant;

import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event.BbEvents;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event.BbPlantEvents;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.BbMobEntity;
import com.lovetropics.minigames.common.content.biodiversity_blitz.explosion.FilteredExplosion;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.Plant;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.PlantCoverage;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record ProximityBombPlantBehavior(double radius) implements IGameBehavior {
	public static final MapCodec<ProximityBombPlantBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.DOUBLE.fieldOf("radius").forGetter(c -> c.radius)
	).apply(i, ProximityBombPlantBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(BbPlantEvents.TICK, (players, plot, plants) -> {
			long ticks = game.ticks();
			if (ticks % 5 != 0) {
				return;
			}

			ServerLevel world = game.level();
			List<Plant> removedPlants = new ArrayList<>();

			for (Plant plant : plants) {
				AABB detonateBounds = plant.coverage().asBounds().inflate(radius);
				List<Mob> entities = world.getEntitiesOfClass(Mob.class, detonateBounds, BbMobEntity.PREDICATE);

				if (!entities.isEmpty()) {
					removedPlants.add(plant);

					explode(world, plant.coverage());
				}
			}

			for (Plant plant : removedPlants) {
				game.invoker(BbEvents.BREAK_PLANT).breakPlant(players.iterator().next(), plot, plant);
			}
		});
	}

	// Kaboom!
	private static void explode(ServerLevel world, PlantCoverage coverage) {
		for (BlockPos pos : coverage) {
			world.removeBlock(pos, true);

			Vec3 center = Vec3.atCenterOf(pos);

			ServerExplosion explosion = new FilteredExplosion(world, null, null, null, center, 2.0f, false, Explosion.BlockInteraction.DESTROY, e -> e instanceof ServerPlayer);
			explosion.explode();

			for (ServerPlayer player : world.players()) {
				if (player.distanceToSqr(center) < 4096.0) {
					Optional<Vec3> knockback = Optional.ofNullable(explosion.getHitPlayers().get(player));
					player.connection.send(new ClientboundExplodePacket(
							center,
							knockback.map(k -> k.scale(2.0f)),
							explosion.isSmall() ? ParticleTypes.EXPLOSION : ParticleTypes.EXPLOSION_EMITTER,
							SoundEvents.GENERIC_EXPLODE
					));
				}
			}
		}
	}
}
