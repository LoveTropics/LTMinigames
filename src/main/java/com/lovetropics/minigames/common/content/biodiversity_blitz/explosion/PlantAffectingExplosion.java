package com.lovetropics.minigames.common.content.biodiversity_blitz.explosion;

import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.Plant;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.state.PlantHealth;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public class PlantAffectingExplosion extends FilteredExplosion {
	private final Plot plot;

	public PlantAffectingExplosion(ServerLevel level, @Nullable Entity source, @Nullable DamageSource damageSource, @Nullable ExplosionDamageCalculator damageCalculator, Vec3 center, float radius, boolean fire, BlockInteraction blockInteraction, Predicate<Entity> remove, Plot plot) {
		super(level, source, damageSource, damageCalculator, center, radius, fire, blockInteraction, remove);
		this.plot = plot;
	}

	public void affectPlants(List<BlockPos> affectedBlocks) {
		Random random = new Random();
		for (BlockPos pos : affectedBlocks) {
			Vec3 vec = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
			double distance = vec.distanceToSqr(center());
			// TODO: damage should scale based on radius
			double damage = 80.0 / (distance + 1);
			// Randomize damage a bit to leave certain plants standing
			damage *= (1 + ((random.nextDouble() - random.nextDouble()) * 0.3));

			Plant plant = plot.plants.getPlantAt(pos);
			if (plant != null) {
				PlantHealth health = plant.state(PlantHealth.KEY);

				if (health != null) {
					health.decrement((int) damage);
				}
			}
		}
	}
}
