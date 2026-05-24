package com.lovetropics.minigames.common.content.biodiversity_blitz.entity.impl;

import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.BbMobEntity;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.ai.BbGroundNavigator;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.ai.BbMobBrain;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.ai.BbTargetPlayerGoal;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.ai.DestroyCropGoal;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.Plant;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.state.PlantHealth;
import com.lovetropics.minigames.common.util.Util;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

public class BbZoglinEntity extends Zoglin implements BbMobEntity {
	private final BbMobBrain mobBrain;
	private final Plot plot;

	private int ticks;

	public BbZoglinEntity(EntityType<? extends Zoglin> pEntityType, Level pLevel, Plot plot) {
		super(pEntityType, pLevel);

		mobBrain = new BbMobBrain(plot.walls);
		this.plot = plot;

		setPathfindingMalus(PathType.DAMAGING_IN_NEIGHBOR, 0.0F);
	}

	@Override
	public boolean navigateBlockGrid() {
		return false;
	}

	@Override
	protected PathNavigation createNavigation(Level world) {
		return new BbGroundNavigator(this);
	}

	@Override
	protected void registerGoals() {
		// Remove Brain, replace with Goal
		removeFreeWill();

		goalSelector.addGoal(3, new DestroyCropGoal(this) {
			@Override
			protected double getDistanceSq(BlockState state) {
				return 2.5 * 2.5;
			}

			@Override
			protected double speed() {
				return 0.5;
			}
		});

		targetSelector.addGoal(1, new BbTargetPlayerGoal(this));
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.is(DamageTypes.PLAYER_ATTACK)) {
			amount /= 2.5f;
		}
		return super.hurtServer(level, source, amount);
	}

	@Override
	public void aiStep() {
		ticks++;
		if (ticks % 7 == 0) {
			Plant plant = getPlot().plants.getPlantAt(blockPosition());

			if (plant != null) {
				PlantHealth health = plant.state(PlantHealth.KEY);

				if (health != null) {
					health.decrement(meleeDamage(random));

					Util.spawnDamageParticles(this, blockPosition(), 4);
				}
			}
		}

		super.aiStep();
	}

	@Override
	protected Vec3 maybeBackOffFromEdge(Vec3 offset, MoverType mover) {
		return mobBrain.getPlotWalls().collide(getBoundingBox(), offset);
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
	public int meleeDamage(RandomSource random) {
		return 12 + BbMobEntity.super.meleeDamage(random);
	}

	@Override
	protected void pushEntities() {
	}

	@Override
	public float aiSpeed() {
		return 1.2f;
	}

	@Override
	public boolean immuneToFire() {
		return true;
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
