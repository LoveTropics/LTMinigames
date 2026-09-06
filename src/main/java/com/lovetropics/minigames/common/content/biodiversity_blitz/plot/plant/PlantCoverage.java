package com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface PlantCoverage extends Iterable<BlockPos> {
	static PlantCoverage of(BlockPos block) {
		return new Single(block);
	}

	static PlantCoverage ofDouble(BlockPos block) {
		LongSet blocks = new LongOpenHashSet(2);
		blocks.add(block.asLong());
		blocks.add(block.above().asLong());
		return new Set(blocks, block);
	}

	static PlantCoverage of(LongSet blocks, BlockPos origin) {
		return new Set(blocks, origin);
	}

	static PlantCoverage or(PlantCoverage left, PlantCoverage right) {
		return new Or(left, right);
	}

	boolean covers(BlockPos pos);

	BlockPos random(RandomSource random);

	AABB asBounds();

	BlockPos getOrigin();

	default void add(BlockPos pos) {
		throw new UnsupportedOperationException("This coverage type cannot be added to");
	}

	default Stream<BlockPos> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	default boolean intersects(PlantCoverage other) {
		if (!asBounds().intersects(other.asBounds())) {
			return false;
		}

		for (BlockPos pos : this) {
			if (other.covers(pos)) {
				return true;
			}
		}

		return false;
	}

	default @Nullable PlantCoverage removeIntersection(LongSet intersection) {
		LongSet blocks = new LongOpenHashSet();
		for (BlockPos pos : this) {
			if (!intersection.contains(pos.asLong())) {
				blocks.add(pos.asLong());
			}
		}

		return !blocks.isEmpty() ? new Set(blocks, getOrigin()) : null;
	}

	final class Single implements PlantCoverage {
		private final BlockPos block;
		private final AABB bounds;

		private Single(BlockPos block) {
			this.block = block;
			bounds = new AABB(block);
		}

		@Override
		public boolean covers(BlockPos pos) {
			return block.equals(pos);
		}

		@Override
		public AABB asBounds() {
			return bounds;
		}

		@Override
		public BlockPos getOrigin() {
			return block;
		}

		@Override
		public BlockPos random(RandomSource random) {
			return block;
		}

		@Override
		public Iterator<BlockPos> iterator() {
			return Iterators.singletonIterator(block);
		}
	}

	final class Set implements PlantCoverage {
		private final LongList blocks;
		private final BlockPos origin;
		private final AABB bounds;

		private Set(LongSet blocks, BlockPos origin) {
			this.blocks = new LongArrayList(blocks);
			this.origin = origin;

			AABB bounds = null;
			for (BlockPos pos : this) {
				AABB blockBounds = new AABB(pos);
				if (bounds == null) {
					bounds = blockBounds;
				} else {
					bounds = bounds.minmax(blockBounds);
				}
			}

			this.bounds = Preconditions.checkNotNull(bounds, "empty plant coverage");
		}

		@Override
		public boolean covers(BlockPos pos) {
			return blocks.contains(pos.asLong());
		}

		@Override
		public AABB asBounds() {
			return bounds;
		}

		@Override
		public BlockPos getOrigin() {
			return origin;
		}

		@Override
		public BlockPos random(RandomSource random) {
			long pos = blocks.getLong(random.nextInt(blocks.size()));
			return new BlockPos(BlockPos.getX(pos), BlockPos.getY(pos), BlockPos.getZ(pos));
		}

		@Override
		public void add(BlockPos pos) {
			if (bounds.contains(Vec3.atCenterOf(pos))) {
				blocks.add(pos.asLong());
			} else {
				throw new IllegalArgumentException("Position not within bounds");
			}
		}

		@Override
		public Iterator<BlockPos> iterator() {
			LongIterator blockIterator = blocks.iterator();

			return new Iterator<>() {
				private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

				@Override
				public BlockPos next() {
					long pos = blockIterator.nextLong();
					return mutablePos.set(pos);
				}

				@Override
				public boolean hasNext() {
					return blockIterator.hasNext();
				}
			};
		}
	}

	final class Or implements PlantCoverage {
		private final PlantCoverage left;
		private final PlantCoverage right;
		private final AABB bounds;

		Or(PlantCoverage left, PlantCoverage right) {
			this.left = left;
			this.right = right;
			bounds = left.asBounds().minmax(right.asBounds());
		}

		@Override
		public boolean covers(BlockPos pos) {
			return left.covers(pos) || right.covers(pos);
		}

		@Override
		public BlockPos random(RandomSource random) {
			return random.nextBoolean() ? left.random(random) : right.random(random);
		}

		@Override
		public AABB asBounds() {
			return bounds;
		}

		@Override
		public BlockPos getOrigin() {
			return left.getOrigin();
		}

		@Override
		public Iterator<BlockPos> iterator() {
			return Iterators.concat(left.iterator(), right.iterator());
		}
	}

	final class Builder {
		private final LongSet blocks = new LongOpenHashSet();
		private @Nullable BlockPos origin;

		public Builder add(BlockPos pos) {
			blocks.add(pos.asLong());
			if (origin == null) {
				origin = pos;
			}
			return this;
		}

		public Builder setOrigin(BlockPos pos) {
			origin = pos;
			return this;
		}

		public PlantCoverage build() {
			if (blocks.isEmpty() || origin == null) {
				throw new IllegalStateException("cannot build empty plant");
			}

			if (blocks.size() == 1) {
				return PlantCoverage.of(BlockPos.of(blocks.iterator().nextLong()));
			} else {
				return PlantCoverage.of(blocks, origin);
			}
		}
	}
}
