package com.lovetropics.minigames.common.content.biodiversity_blitz.entity.ai;

import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.BbMobEntity;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.Plant;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.state.PlantHealth;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.state.PlantNotPathfindable;
import com.lovetropics.minigames.common.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class DestroyCropGoal extends MoveToBlockGoal {
	private static final int DAMAGE_INTERVAL = 20;

	private int ticksAtTarget = DAMAGE_INTERVAL;

	public DestroyCropGoal(BbMobEntity mob) {
		super(mob);
	}

	@Override
	public void tick() {
		super.tick();

		double distance2 = mob.position().distanceToSqr(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);
		if (distance2 <= getDistanceSq(mob.level().getBlockState(targetPos))) {
			ticksAtTarget--;
			if (mob.level().random.nextInt(4) == 0) {
				Util.spawnDamageParticles(mob, targetPos, 0);
			}

			if (ticksAtTarget <= 0) {
				ticksAtTarget = DAMAGE_INTERVAL;
				tryDamagePlant(mob);
			}
		} else {
			ticksAtTarget = DAMAGE_INTERVAL;
		}
	}

	protected double getDistanceSq(BlockState state) {
		return 1.75 * 1.75;
	}

	protected void tryDamagePlant(Mob mob) {
		Plant plant = bbMob.getPlot().plants.getPlantAt(targetPos);
		if (plant != null) {
			PlantHealth health = plant.state(PlantHealth.KEY);

			if (health != null) {
				int damage = bbMob.meleeDamage(mob.level().getRandom());
				health.decrement(damage);

				Util.spawnDamageParticles(mob, targetPos, damage);
			}
		}
	}

	@Override
	protected int getBlockPriority(BlockPos pos, Plant plant) {
		// Always path towards visible pumpkins
		BlockState state = bbMob.asMob().level().getBlockState(pos);
		if (state.is(Blocks.PUMPKIN) || state.is(Blocks.CARVED_PUMPKIN)) {
			return 10;
		}

		if (plant.state(PlantHealth.KEY) != null) {

			// We want to prioritize plants that are pathfindable.
			// This ensures that if by chance a player has a plot that's 100% grass + berry bushes,
			// the mobs will still get to them.

			if (plant.state(PlantNotPathfindable.KEY) == null) {
				return 2;
			} else {
				return 1;
			}
		}

		return 0;
	}
}
