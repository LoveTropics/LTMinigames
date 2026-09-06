package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameLivingEntityEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressChannel;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressHolder;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressionSpline;
import com.lovetropics.minigames.common.core.game.util.FluidFiller;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

import org.jspecify.annotations.Nullable;
import java.util.function.DoubleSupplier;

public class RisingFluidBehavior implements IGameBehavior {
    public static final MapCodec<RisingFluidBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("region").forGetter(c -> c.regionKey),
            ProgressionSpline.CODEC.fieldOf("fluid_levels").forGetter(c -> c.fluidLevels),
            FluidFiller.Type.CODEC.fieldOf("fill_type").forGetter(b -> b.fillType)
    ).apply(i, RisingFluidBehavior::new));

    private final String regionKey;
    private final ProgressionSpline fluidLevels;
    private final FluidFiller.Type fillType;

    private DoubleSupplier targetFluidLevel = () -> 0.0;

    private int fluidLevel;
    private @Nullable FluidFiller filler;

    public RisingFluidBehavior(String regionKey, ProgressionSpline fluidLevels, FluidFiller.Type fillType) {
        this.regionKey = regionKey;
        this.fluidLevels = fluidLevels;
        this.fillType = fillType;
    }

    @Override
    public void register(IGamePhase game, EventRegistrar events) throws GameException {
        ProgressHolder progression = ProgressChannel.MAIN.getOrThrow(game);
        targetFluidLevel = fluidLevels.resolve(progression);

        events.listen(GamePhaseEvents.START, initiator -> {
            int initialFluidLevel = Mth.floor(targetFluidLevel.getAsDouble());
            fluidLevel = initialFluidLevel;
            filler = new FluidFiller(game.mapRegions().getOrThrow(regionKey), fillType, initialFluidLevel);
        });

        if (fillType == FluidFiller.Type.WATER) {
            events.listen(GameLivingEntityEvents.TICK, this::onLivingUpdateInWater);
        }
        events.listen(GamePhaseEvents.TICK, () -> tick(game));
    }

    private void onLivingUpdateInWater(LivingEntity entity) {
        if (filler == null) {
            return;
        }

        // NOTE: DO NOT REMOVE THIS CHECK, CAUSES FISH TO DIE AND SPAWN ITEMS ON DEATH
        // FISH WILL KEEP SPAWNING, DYING AND COMPLETELY SLOW THE SERVER TO A CRAWL
        if (!entity.canBreatheUnderwater()) {
            if (entity.getY() <= filler.fluidLevel() + 1 && entity.isInWater() && entity.tickCount % 40 == 0) {
                entity.hurt(entity.damageSources().drown(), 2.0F);
            }
        }
    }

    private void tick(IGamePhase game) {
        if (filler != null) {
            int targetFluidLevel = Mth.floor(this.targetFluidLevel.getAsDouble());
            if (fluidLevel < targetFluidLevel) {
                fluidLevel++;
            }
            filler.tick(game, fluidLevel);
            if (fillType == FluidFiller.Type.WATER) {
                spawnWarningParticles(game);
            }
        }
    }

    private void spawnWarningParticles(IGamePhase game) {
        ServerLevel world = game.level();
        RandomSource random = world.getRandom();
        if (random.nextInt(3) != 0) {
            return;
        }

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (ServerPlayer player : game.participants()) {
            // only attempt to spawn particles if the player is near the water surface
            if (Math.abs(player.getY() - fluidLevel) > 5) {
                continue;
            }

            int particleX = Mth.floor(player.getX()) - random.nextInt(5) + random.nextInt(5);
            int particleZ = Mth.floor(player.getZ()) - random.nextInt(5) + random.nextInt(5);
            mutablePos.set(particleX, fluidLevel, particleZ);

            if (!world.isEmptyBlock(mutablePos) && world.isEmptyBlock(mutablePos.move(Direction.UP))) {
                Packet<?> packet = new ClientboundLevelParticlesPacket(ParticleTypes.SPLASH, false, false, particleX, fluidLevel + 1, particleZ, 0.1F, 0.0F, 0.1F, 0.0F, 4);
                player.connection.send(packet);
            }
        }
    }
}
