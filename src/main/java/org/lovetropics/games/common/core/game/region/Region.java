package org.lovetropics.games.common.core.game.region;

import com.lovetropics.lib.BlockBox;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.stream.Stream;

public record Region(
		ServerLevel level,
		BlockBox box
) implements Iterable<BlockPos> {
	public BlockPos min() {
		return box.min();
	}

	public BlockPos max() {
		return box.max();
	}

	public BlockPos centerBlock() {
		return box.centerBlock();
	}

	public Vec3 center() {
		return box.center();
	}

	public LongSet chunks() {
		return box.asChunks();
	}

	public Vec3i size() {
		return box.size();
	}

	public AABB aabb() {
		return box.asAabb();
	}

	public boolean contains(Entity entity) {
		return contains(entity.level(), entity.position());
	}

	public boolean contains(Level level, BlockPos pos) {
		return contains(level, pos.getX(), pos.getY(), pos.getZ());
	}

	public boolean contains(Level level, int x, int y, int z) {
		return this.level == level && box.contains(x, y, z);
	}

	public boolean contains(Level level, Vec3 pos) {
		return this.level == level && box.contains(pos);
	}

	public boolean intersects(Level level, AABB aabb) {
		return this.level == level && box.intersects(aabb);
	}

	public BlockPos sample(RandomSource random) {
		return box.sample(random);
	}

	public Region offset(int x, int y, int z) {
		return new Region(level, box.offset(x, y, z));
	}

	@Override
	public Iterator<BlockPos> iterator() {
		return box.iterator();
	}

	public Stream<BlockPos> blockStream() {
		return BlockPos.betweenClosedStream(box.min(), box.max());
	}
}
