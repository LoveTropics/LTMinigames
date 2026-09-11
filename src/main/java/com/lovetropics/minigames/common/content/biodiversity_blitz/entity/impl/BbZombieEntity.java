package com.lovetropics.minigames.common.content.biodiversity_blitz.entity.impl;

import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.BbMobEntity;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.ai.BbGroundNavigator;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.ai.BbMobBrain;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.ai.BbTargetPlayerGoal;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.ai.DestroyCropGoal;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import com.lovetropics.minigames.common.util.duck.ClearableFluidInteraction;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import org.jspecify.annotations.Nullable;

public class BbZombieEntity extends Zombie implements BbMobEntity {
	private final BbMobBrain mobBrain;
	private final Plot plot;

	public BbZombieEntity(EntityType<? extends Zombie> type, Level world, Plot plot) {
		super(type, world);
		mobBrain = new BbMobBrain(plot.walls);
		this.plot = plot;

		// Ignore sweet berry bushes and water
		setPathfindingMalus(PathType.DAMAGING_IN_NEIGHBOR, BERRY_BUSH_MALUS);
	}

	@Override
	protected PathNavigation createNavigation(Level world) {
		return new BbGroundNavigator(this);
	}

	@Override
	protected void addBehaviourGoals() {
		goalSelector.addGoal(2, new ZombieAttackGoal(this, BbMobEntity.ATTACK_MOVE_SPEED, false));
		goalSelector.addGoal(3, new DestroyCropGoal(this));

		targetSelector.addGoal(1, new BbTargetPlayerGoal(this));
	}

	@Override
	protected Vec3 maybeBackOffFromEdge(Vec3 offset, MoverType mover) {
		return mobBrain.getPlotWalls().collide(getBoundingBox(), offset);
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData spawnData) {
		setLeftHanded(random.nextFloat() < 0.05F);
		return spawnData;
	}

	@Override
	public void burnUndead() {

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
	public void updateSwimming() {
		// Just use the default navigator, we never need to swim
	}

	@Override
	protected boolean updateFluidInteraction() {
		super.updateFluidInteraction();
		((ClearableFluidInteraction) getFluidInteraction()).ltminigames$removeFluid(NeoForgeMod.WATER_TYPE.value());
		wasTouchingWater = false;
		return getFluidInteraction().isInAnyFluid();
	}

	@Override
	public boolean isEyeInFluid(TagKey<Fluid> fluid) {
		if (fluid == FluidTags.WATER) {
			return false;
		}
		return super.isEyeInFluid(fluid);
	}
}
