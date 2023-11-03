package com.lovetropics.minigames.common.content.biodiversity_blitz.entity.impl;

import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.BbMobEntity;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.ai.BbGroundNavigator;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.ai.BbMobBrain;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.ai.BbTargetPlayerGoal;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.ai.DestroyCropGoal;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;

public class BbVindicatorEntity extends Vindicator implements BbMobEntity {
    private final BbMobBrain mobBrain;
    private final Plot plot;

    public BbVindicatorEntity(EntityType<? extends Vindicator> pEntityType, Level pLevel, Plot plot) {
        super(pEntityType, pLevel);

        this.mobBrain = new BbMobBrain(plot.walls);
        this.plot = plot;

        // Ignore sweet berry bushes and water
        setPathfindingMalus(BlockPathTypes.DANGER_OTHER, BERRY_BUSH_MALUS);
        setPathfindingMalus(BlockPathTypes.WATER, -1.0F);
    }

    @Override
    protected PathNavigation createNavigation(Level world) {
        return new BbGroundNavigator(this);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(3, new DestroyCropGoal(this));

        this.targetSelector.addGoal(1, new BbTargetPlayerGoal(this));
    }

    @Override
    protected Vec3 maybeBackOffFromEdge(Vec3 offset, MoverType mover) {
        return mobBrain.getPlotWalls().collide(this.getBoundingBox(), offset);
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        return super.hurt(pSource, pAmount / 2);
    }

    @Override
    public BbMobBrain getMobBrain() {
        return this.mobBrain;
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
    public int meleeDamage(RandomSource random) {
        return 8 + BbMobEntity.super.meleeDamage(random);
    }

    @Override
    protected void pushEntities() {
    }
}