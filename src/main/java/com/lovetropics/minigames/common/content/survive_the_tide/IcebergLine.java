package com.lovetropics.minigames.common.content.survive_the_tide;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

import java.util.Random;

public class IcebergLine {
	public static final MapCodec<IcebergLine> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			BlockPos.CODEC.fieldOf("posA").forGetter(c -> c.start),
			BlockPos.CODEC.fieldOf("posB").forGetter(c -> c.end),
			Codec.INT.optionalFieldOf("distanceBetweenEach", 10).forGetter(c -> c.distBetweenEach)
	).apply(i, IcebergLine::new));

	private final BlockPos start, end;

	private final int count;
	private final int intervalX;
	private final int intervalZ;
	private final int distBetweenEach;

	private final Random rand;

	public IcebergLine(BlockPos start, BlockPos end, int distBetweenEach) {
		rand = new Random();
		this.distBetweenEach = distBetweenEach;

		this.start = start;
		this.end = end;

		int diffX = this.start.getX() - this.end.getX();
		int diffZ = this.start.getZ() - this.end.getZ();

		count = Math.max(1, Math.max(Math.abs(diffX), Math.abs(diffZ)) / distBetweenEach);

		intervalX = Math.round((float) diffX / (float) count);
		intervalZ = Math.round((float) diffZ / (float) count);
	}

	public void grow(Level level) {
		for (int i = 1; i <= count; i++) {
			int offsetX = getRandOffset(distBetweenEach);
			int offsetZ = getRandOffset(distBetweenEach);

			BlockPos pos = new BlockPos(start.getX() - (i * intervalX) + offsetX, start.getY(), start.getZ() - (i * intervalZ) + offsetZ);
			setIceWithCheck(level, pos);
		}

		BlockPos start = this.start.offset(
				getRandOffset(distBetweenEach),
				0,
				getRandOffset(distBetweenEach)
		);

		BlockPos end = this.start.offset(
				getRandOffset(distBetweenEach),
				0,
				getRandOffset(distBetweenEach)
		);

		setIceWithCheck(level, start);
		setIceWithCheck(level, end);
	}

	private int getRandOffset(int radius) {
		return rand.nextInt(radius) * (rand.nextBoolean() ? -1 : 1);
	}

	private void setIceWithCheck(Level level, BlockPos pos) {
		if (level.getFluidState(pos).is(Fluids.WATER)) {
			level.setBlock(pos, Blocks.SNOW_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
	}
}
