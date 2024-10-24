package com.lovetropics.minigames.common.content.biodiversity_blitz.entity.impl;

import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.BbMobEntity;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.ai.BbGroundNavigator;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.ai.BbMobBrain;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.ai.BbTargetPlayerGoal;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.ai.DestroyCropGoal;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

public class BbVindicatorEntity extends Vindicator implements BbMobEntity {
    private final BbMobBrain mobBrain;
    private final Plot plot;

    public BbVindicatorEntity(EntityType<? extends Vindicator> pEntityType, Level pLevel, Plot plot) {
        super(pEntityType, pLevel);

        mobBrain = new BbMobBrain(plot.walls);
        this.plot = plot;

        // Ignore sweet berry bushes and water
        setPathfindingMalus(PathType.DANGER_OTHER, BERRY_BUSH_MALUS);
        setPathfindingMalus(PathType.WATER, -1.0F);
    }

    @Override
    protected PathNavigation createNavigation(Level world) {
        return new BbGroundNavigator(this);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(3, new DestroyCropGoal(this));

        targetSelector.addGoal(1, new BbTargetPlayerGoal(this));
    }

    @Override
    protected Vec3 maybeBackOffFromEdge(Vec3 offset, MoverType mover) {
        return mobBrain.getPlotWalls().collide(getBoundingBox(), offset);
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (pSource.is(DamageTypes.PLAYER_ATTACK)) {
            pAmount /= 2;
        }

        return super.hurt(pSource, pAmount);
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
    public float aiSpeed() {
        return 1.0f;
    }

    @Override
    public int meleeDamage(RandomSource random) {
        return 8 + BbMobEntity.super.meleeDamage(random);
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
