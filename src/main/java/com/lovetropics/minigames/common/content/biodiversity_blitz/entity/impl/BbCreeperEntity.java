package com.lovetropics.minigames.common.content.biodiversity_blitz.entity.impl;

import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.BbMobEntity;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.ai.BbGroundNavigator;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.ai.BbMobBrain;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.ai.KaboomCropGoal;
import com.lovetropics.minigames.common.content.biodiversity_blitz.explosion.PlantAffectingExplosion;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

public class BbCreeperEntity extends Creeper implements BbMobEntity {
    private final BbMobBrain mobBrain;
    private final Plot plot;
    private float explosionSizeOffset = 0;

    
    public BbCreeperEntity(EntityType<? extends Creeper> type, Level world, Plot plot) {
        super(type, world);
        mobBrain = new BbMobBrain(plot.walls);
        this.plot = plot;

        setPathfindingMalus(PathType.DANGER_OTHER, BERRY_BUSH_MALUS);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new KaboomCropGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    public void setCreeperExplodeSizeOffset(float off) {
        explosionSizeOffset = off;
    }

    @Override
    public void aiStep() {
        if (getHealth() < 8) {
            setSwellDir(1);
        }

        super.aiStep();
    }

    @Override
    public void explodeCreeper() {
        if (!level().isClientSide) {
            double x = getX();
            double y = getY();
            double z = getZ();

            float size = 2.5f + explosionSizeOffset;
            Explosion explosion = new PlantAffectingExplosion(level(), null, null, null, x, y, z, size, false, Explosion.BlockInteraction.DESTROY, ParticleTypes.EXPLOSION, ParticleTypes.EXPLOSION_EMITTER, SoundEvents.GENERIC_EXPLODE, e -> true, plot);
            explosion.explode();
            explosion.finalizeExplosion(false);

            float factor = isPowered() ? 2.0F : 1.0F;
            for (ServerPlayer player : ((ServerLevel) level()).players()) {
                if (player.distanceToSqr(x, y, z) < 4096.0) {
                    player.connection.send(new ClientboundExplodePacket(x, y, z, size * factor, explosion.getToBlow(), explosion.getHitPlayers().get(player), explosion.getBlockInteraction(), explosion.getSmallExplosionParticles(), explosion.getLargeExplosionParticles(), explosion.getExplosionSound()));
                }
            }

            remove(RemovalReason.KILLED);
        }
    }

    @Override
    protected Vec3 maybeBackOffFromEdge(Vec3 offset, MoverType mover) {
        return mobBrain.getPlotWalls().collide(getBoundingBox(), offset);
    }

    @Override
    protected PathNavigation createNavigation(Level world) {
        return new BbGroundNavigator(this);
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
        return 0.8f;
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
