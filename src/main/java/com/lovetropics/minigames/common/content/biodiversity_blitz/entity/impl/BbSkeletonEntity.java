package com.lovetropics.minigames.common.content.biodiversity_blitz.entity.impl;

import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.BbMobEntity;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.ai.BbMobBrain;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.ai.BbTargetPlayerGoal;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.ai.DestroyCropGoal;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.pathfinder.PathType;

public class BbSkeletonEntity extends Skeleton implements BbMobEntity {
	private final BbMobBrain mobBrain;
	private final Plot plot;

	public BbSkeletonEntity(EntityType<? extends Skeleton> entityType, Level level, Plot plot) {
		super(entityType, level);

		mobBrain = new BbMobBrain(plot.walls);
		this.plot = plot;

		setPathfindingMalus(PathType.DAMAGING_IN_NEIGHBOR, BERRY_BUSH_MALUS);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(3, new DestroyCropGoal(this));
		goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 15.0F, 0.02F));

		targetSelector.addGoal(1, new BbTargetPlayerGoal(this));

		// Setup bow goal

		// TODO: is this the right spot for this?
		populateDefaultEquipmentSlots(random, new DifficultyInstance(Difficulty.NORMAL, 0, 0, 0));
		reassessWeaponGoal();
	}

	@Override
	public BbMobBrain getMobBrain() {
		return mobBrain;
	}

	@Override
	public Mob asMob() {
		return this;
	}

	@Override
	public Plot getPlot() {
		return plot;
	}

	@Override
	protected void pushEntities() {
	}

	@Override
	public void updateSwimming() {
		// Just use the default navigator, we never need to swim
	}

	@Override
	public boolean updateFluidHeightAndDoFluidPushing(TagKey<Fluid> fluid, double scale) {
		if (fluid == FluidTags.WATER) {
			return false;
		}
		return super.updateFluidHeightAndDoFluidPushing(fluid, scale);
	}

	@Override
	public boolean isEyeInFluid(TagKey<Fluid> fluid) {
		if (fluid == FluidTags.WATER) {
			return false;
		}
		return super.isEyeInFluid(fluid);
	}
}
