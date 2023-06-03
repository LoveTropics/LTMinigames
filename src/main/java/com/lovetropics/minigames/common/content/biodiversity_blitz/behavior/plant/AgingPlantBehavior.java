package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant;

import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event.BbPlantEvents;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.Plant;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;

public abstract class AgingPlantBehavior implements IGameBehavior {
    protected final int interval;

    public AgingPlantBehavior(int interval) {
        this.interval = interval;
    }

    @Override
    public void register(IGamePhase game, EventRegistrar events) {
        events.listen(BbPlantEvents.TICK, (players, plot, plants) -> {
            long ticks = game.ticks();
            if (ticks % this.interval != 0) {
                return;
            }

            ServerLevel world = game.getWorld();

            for (Plant plant : plants) {
                for (BlockPos pos : plant.coverage()) {
                    BlockState state = world.getBlockState(pos);
                    BlockState agedState = ageUp(world.random, state);

                    if (state != agedState) {
                        for (BlockPos plantPos : plant.coverage()) {
                            world.levelEvent(LevelEvent.PARTICLES_PLANT_GROWTH, plantPos, 0);
                        }

                        world.setBlockAndUpdate(pos, agedState);
                    }
                }
            }
        });
    }

    protected abstract BlockState ageUp(RandomSource random, BlockState state);
}
