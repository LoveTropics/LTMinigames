package org.lovetropics.games.common.content.survive_the_tide;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

public class IcebergLine {
	private final ServerLevel level;
	private final BlockPos start;

	private final int count;
	private final int intervalX;
	private final int intervalZ;
	private final int distBetweenEach;

	private final RandomSource rand = RandomSource.create();

	public IcebergLine(ServerLevel level, BlockPos start, BlockPos end, int distBetweenEach) {
		this.level = level;
		this.distBetweenEach = distBetweenEach;

		this.start = start;

		int diffX = this.start.getX() - end.getX();
		int diffZ = this.start.getZ() - end.getZ();

		count = Math.max(1, Math.max(Math.abs(diffX), Math.abs(diffZ)) / distBetweenEach);

		intervalX = Math.round((float) diffX / (float) count);
		intervalZ = Math.round((float) diffZ / (float) count);
	}

	public void grow() {
		for (int i = 1; i <= count; i++) {
			int offsetX = getRandOffset(distBetweenEach);
			int offsetZ = getRandOffset(distBetweenEach);

			BlockPos pos = new BlockPos(start.getX() - (i * intervalX) + offsetX, start.getY(), start.getZ() - (i * intervalZ) + offsetZ);
			setIceWithCheck(pos);
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

		setIceWithCheck(start);
		setIceWithCheck(end);
	}

	private int getRandOffset(int radius) {
		return rand.nextInt(radius) * (rand.nextBoolean() ? -1 : 1);
	}

	private void setIceWithCheck(BlockPos pos) {
		if (level.getFluidState(pos).is(Fluids.WATER)) {
			level.setBlock(pos, Blocks.SNOW_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
	}
}
