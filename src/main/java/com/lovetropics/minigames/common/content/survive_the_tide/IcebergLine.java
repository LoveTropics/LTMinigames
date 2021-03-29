package com.lovetropics.minigames.common.content.survive_the_tide;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

public class IcebergLine
{
    public static final Codec<IcebergLine> CODEC = RecordCodecBuilder.create(instance -> {
        return instance.group(
                BlockPos.CODEC.fieldOf("posA").forGetter(c -> c.start),
                BlockPos.CODEC.fieldOf("posB").forGetter(c -> c.end),
                Codec.INT.optionalFieldOf("distanceBetweenEach", 10).forGetter(c -> c.distBetweenEach)
        ).apply(instance, IcebergLine::new);
    });

    private final BlockPos start, end;

    private final int count;
    private final int intervalX;
    private final int intervalZ;
    private final int distBetweenEach;

    private final Random rand;

    public IcebergLine(BlockPos start, BlockPos end, int distBetweenEach) {
        this.rand = new Random();
        this.distBetweenEach = distBetweenEach;

        this.start = start;
        this.end = end;

        int diffX = this.start.getX() - this.end.getX();
        int diffZ = this.start.getZ() - this.end.getZ();

        this.count = Math.max(1, Math.max(Math.abs(diffX), Math.abs(diffZ)) / distBetweenEach);

        this.intervalX = Math.round((float)diffX / (float)count);
        this.intervalZ = Math.round((float)diffZ / (float)count);
    }

    public void generate(World world, int waterLevel) {
        for (int i = 1; i <= count; i++) {
            int offsetX = getRandOffset(this.distBetweenEach);
            int offsetZ = getRandOffset(this.distBetweenEach);

            BlockPos pos = new BlockPos(this.start.getX() - (i * intervalX) + offsetX, waterLevel, this.start.getZ() - (i * intervalZ) + offsetZ);

            setIceWithCheck(world, pos);
        }

        BlockPos start = new BlockPos(
                this.start.getX() + getRandOffset(this.distBetweenEach),
                waterLevel,
                this.start.getZ() + getRandOffset(this.distBetweenEach));


        BlockPos end = new BlockPos(
                this.start.getX() + getRandOffset(this.distBetweenEach),
                waterLevel,
                this.start.getZ() + getRandOffset(this.distBetweenEach));

        setIceWithCheck(world, start);
        setIceWithCheck(world, end);
    }

    private int getRandOffset(int radius) {
        return rand.nextInt(radius) * (rand.nextBoolean() ? -1 : 1);
    }

    private void setIceWithCheck(World world, BlockPos pos) {
        if (world.getBlockState(pos).getMaterial() == Material.WATER) {
            world.setBlockState(pos, Blocks.SNOW_BLOCK.getDefaultState(), 2);
        }
    }
}
