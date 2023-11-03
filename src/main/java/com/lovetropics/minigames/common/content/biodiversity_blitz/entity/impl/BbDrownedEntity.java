package com.lovetropics.minigames.common.content.biodiversity_blitz.entity.impl;

import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.BbMobEntity;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.ai.BbGroundNavigator;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.ai.BbMobBrain;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.ai.BbTargetPlayerGoal;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.ai.DestroyCropGoal;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class BbDrownedEntity extends Drowned implements BbMobEntity {
	private final BbMobBrain mobBrain;
	private final Plot plot;

	public BbDrownedEntity(EntityType<? extends Drowned> type, Level world, Plot plot) {
		super(type, world);
		this.mobBrain = new BbMobBrain(plot.walls);
		this.plot = plot;

		setPathfindingMalus(BlockPathTypes.DANGER_OTHER, BERRY_BUSH_MALUS);
	}

	@Override
	protected PathNavigation createNavigation(Level world) {
		return new BbGroundNavigator(this);
	}

	@Override
	protected void addBehaviourGoals() {
		this.goalSelector.addGoal(2, new ZombieAttackGoal(this, BbMobEntity.ATTACK_MOVE_SPEED, false));
		this.goalSelector.addGoal(3, new DestroyCropGoal(this));

		this.targetSelector.addGoal(1, new BbTargetPlayerGoal(this));
	}

	@Override
	protected Vec3 maybeBackOffFromEdge(Vec3 offset, MoverType mover) {
		return mobBrain.getPlotWalls().collide(this.getBoundingBox(), offset);
	}

	@Nullable
	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
		this.setLeftHanded(this.random.nextFloat() < 0.05F);
		return spawnData;
	}

	@Override
	protected boolean isSunSensitive() {
		return false;
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
		return this.plot;
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

	@Override
	protected void pushEntities() {
	}
}
