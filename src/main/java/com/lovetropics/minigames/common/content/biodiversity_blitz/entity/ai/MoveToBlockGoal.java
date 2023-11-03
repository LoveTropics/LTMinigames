package com.lovetropics.minigames.common.content.biodiversity_blitz.entity.ai;

import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.BbMobEntity;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.Plant;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.PlantMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import javax.annotation.Nullable;
import java.util.EnumSet;

public abstract class MoveToBlockGoal extends Goal {
    protected final BbMobEntity bbMob;
    protected final Mob mob;
    // State
    protected BlockPos targetPos;

    protected MoveToBlockGoal(BbMobEntity mob) {
        this.mob = mob.asMob();
        bbMob = mob;

        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        Plant target = locatePlant();
        if (target != null) {
            this.targetPos = target.coverage().getOrigin();
            return true;
        } else {
            return false;
        }
    }

    protected double speed() {
        return bbMob.aiSpeed();
    }

    @Override
    public void start() {
        this.mob.getNavigation().moveTo(this.targetPos.getX(), this.targetPos.getY(), this.targetPos.getZ(), speed());
    }

    @Override
    public boolean canContinueToUse() {
        Plant plant = bbMob.getPlot().plants.getPlantAt(targetPos);
        if (plant == null || getBlockPriority(this.targetPos, plant) > 0) {
            return false;
        }

        return !this.mob.getNavigation().isDone();
    }

    @Nullable
    protected Plant locatePlant() {
        PlantMap plants = bbMob.getPlot().plants;
        Plant maxPrioPlant = null;
        int maxPrio = 0;
        double maxPrioDistSq = Double.MAX_VALUE;

        for (Plant plant : plants) {
            BlockPos plantPos = plant.coverage().getOrigin();
            int prio = getBlockPriority(plantPos, plant);
            double distSq = plantPos.distToCenterSqr(mob.position());
            if (prio > maxPrio) {
                // If the found block has a higher priority, let's unconditionally go to it.
                maxPrio = prio;
                maxPrioPlant = plant;
                maxPrioDistSq = distSq;
            } else if (prio == maxPrio && maxPrioPlant != null && distSq < maxPrioDistSq) {
                // Same priority, but closer? Let's play fair and go to the closer one first.
                maxPrioPlant = plant;
                maxPrioDistSq = distSq;
            }
        }

        return maxPrioPlant;
    }

    // 0: Do not target this block
    // Anything higher will set the target to the current block, based on the current priority
    protected abstract int getBlockPriority(BlockPos pos, Plant plant);
}
