package com.lovetropics.minigames.common.content.biodiversity_blitz.entity.ai;

import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.impl.BbCreeperEntity;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.Plant;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.state.PlantNotPathfindable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;

public class KaboomCropGoal extends DestroyCropGoal {
	private final BbCreeperEntity mob;

	public KaboomCropGoal(BbCreeperEntity mob) {
		super(mob);
		this.mob = mob;
	}

	@Override
	protected double getDistanceSq(BlockState state) {
		// Longer reach distance
		return 2 * 2;
	}

	@Override
	protected void tryDamagePlant(Mob mob) {
		int inc = this.mob.getPlot().nextCurrencyIncrement;
		float offset = 0;

		// Scale the creeper explode size based on how well the players are doing
		if (inc < 2) {
			offset = -0.25f;
		} else if (inc >= 4) {
			offset = 1.0f;
		} else if (inc == 3) {
			offset = 0.5f;
		}

		this.mob.setCreeperExplodeSizeOffset(offset);

		// Tell creeper to explode
		this.mob.setSwellDir(1);
	}

	@Override
	protected int getBlockPriority(BlockPos pos, Plant plant) {
		PlantNotPathfindable state = plant.state(PlantNotPathfindable.KEY);
		if (state != null) {
			return 2;
		}

		return 5;
	}
}
