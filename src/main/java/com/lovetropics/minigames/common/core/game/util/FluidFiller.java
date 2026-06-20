package com.lovetropics.minigames.common.core.game.util;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.network.FillFluidPacket;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrays;
import it.unimi.dsi.fastutil.longs.LongComparator;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jspecify.annotations.Nullable;

public class FluidFiller {
	public static final int HIGH_PRIORITY_BUDGET_PER_TICK = 40;
	public static final int LOW_PRIORITY_BUDGET_PER_TICK = 8;

	public static final int HIGH_PRIORITY_DISTANCE_SQ = 64 * 64;

	private final BlockBox region;
	private final ChunkPos minChunk;
	private final ChunkPos maxChunk;
	private final FluidFiller.Type fillType;

	private int fluidLevel;

	private final LongSet highPriorityUpdates = new LongLinkedOpenHashSet();
	private final LongSet lowPriorityUpdates = new LongLinkedOpenHashSet();
	private final Long2IntMap fluidLevelByChunk = new Long2IntOpenHashMap();

	public FluidFiller(BlockBox region, Type fillType, int baseFluidLevel) {
		this.region = region;
		minChunk = ChunkPos.containing(region.min());
		maxChunk = ChunkPos.containing(region.max());
		this.fillType = fillType;
		fluidLevel = baseFluidLevel;
		fluidLevelByChunk.defaultReturnValue(baseFluidLevel);
	}

	public void tick(IGamePhase game, int targetFluidLevel) {
		if (fluidLevel != targetFluidLevel) {
			fluidLevel = targetFluidLevel;

			boolean close = true;
			for (long chunkPos : collectSortedChunks(game)) {
				if (close) {
					highPriorityUpdates.add(chunkPos);
					int distanceSq = getChunkDistanceSq(game, ChunkPos.getX(chunkPos), ChunkPos.getZ(chunkPos));
					if (distanceSq >= HIGH_PRIORITY_DISTANCE_SQ) {
						close = false;
					}
				} else {
					lowPriorityUpdates.add(chunkPos);
				}
			}
		}

		processUpdates(game);
	}

	public int fluidLevel() {
		return fluidLevel;
	}

	private void processUpdates(IGamePhase game) {
		if (highPriorityUpdates.isEmpty() && lowPriorityUpdates.isEmpty()) {
			return;
		}

		int count = processUpdateQueue(game, highPriorityUpdates.iterator(), HIGH_PRIORITY_BUDGET_PER_TICK);
		if (count <= 0) {
			processUpdateQueue(game, lowPriorityUpdates.iterator(), LOW_PRIORITY_BUDGET_PER_TICK);
		}
	}

	private int processUpdateQueue(IGamePhase game, LongIterator iterator, int maxToProcess) {
		ServerLevel level = game.level();
		ServerChunkCache chunkProvider = level.getChunkSource();

		int count = 0;

		while (count < maxToProcess && iterator.hasNext()) {
			long chunkPos = iterator.nextLong();
			int chunkX = ChunkPos.getX(chunkPos);
			int chunkZ = ChunkPos.getZ(chunkPos);

			// we only want to apply updates to loaded chunks
			LevelChunk chunk = chunkProvider.getChunkNow(chunkX, chunkZ);
			if (chunk == null) {
				continue;
			}

			iterator.remove();
			increaseInChunk(level, chunk);
			count++;
		}

		return count;
	}

	private long[] collectSortedChunks(IGamePhase game) {
		LongComparator distanceComparator = (pos1, pos2) -> Integer.compare(
				getChunkDistanceSq(game, ChunkPos.getX(pos1), ChunkPos.getZ(pos1)),
				getChunkDistanceSq(game, ChunkPos.getX(pos2), ChunkPos.getZ(pos2))
		);

		int sizeX = maxChunk.x() - minChunk.x() + 1;
		int sizeZ = maxChunk.z() - minChunk.z() + 1;

		long[] chunks = new long[sizeX * sizeZ];

		int i = 0;
		for (int z = minChunk.z(); z <= maxChunk.z(); z++) {
			for (int x = minChunk.x(); x <= maxChunk.x(); x++) {
				chunks[i++] = ChunkPos.pack(x, z);
			}
		}

		LongArrays.unstableSort(chunks, distanceComparator);

		return chunks;
	}

	private int getChunkDistanceSq(IGamePhase game, int x, int z) {
		int minDistanceSq = Integer.MAX_VALUE;
		int centerX = SectionPos.sectionToBlockCoord(x) + SectionPos.SECTION_HALF_SIZE;
		int centerZ = SectionPos.sectionToBlockCoord(z) + SectionPos.SECTION_HALF_SIZE;
		for (ServerPlayer player : game.allPlayers()) {
			int dx = player.getBlockX() - centerX;
			int dz = player.getBlockZ() - centerZ;
			int distanceSq = dx * dx + dz * dz;
			if (distanceSq < minDistanceSq) {
				minDistanceSq = distanceSq;
			}
		}
		return minDistanceSq;
	}

	public long increaseInChunk(ServerLevel level, LevelChunk chunk) {
		ChunkPos chunkPos = chunk.getPos();

		int targetLevel = fluidLevel;
		int lastLevel = fluidLevelByChunk.put(chunkPos.pack(), targetLevel);

		if (targetLevel > lastLevel) {
			BlockPos min = region.min();
			BlockPos max = region.max();
			long count = FluidFiller.fillChunk(fillType, min.getX(), min.getZ(), max.getX(), max.getZ(), chunk, lastLevel, targetLevel);
			if (count > 0) {
				PacketDistributor.sendToPlayersTrackingChunk(level, chunk.getPos(), new FillFluidPacket(
						fillType,
						new BlockPos(Math.max(min.getX(), chunkPos.getMinBlockX()), lastLevel, Math.max(min.getZ(), chunkPos.getMinBlockZ())),
						new BlockPos(Math.min(max.getX(), chunkPos.getMaxBlockX()), targetLevel, Math.min(max.getZ(), chunkPos.getMaxBlockZ()))
				));
			}
			return count;
		} else {
			return 0;
		}
	}

	public static long fillChunk(Type type, int minX, int minZ, int maxX, int maxZ, LevelChunk chunk, int fromY, int toY) {
		Level level = chunk.getLevel();
		LevelLightEngine lightEngine = level.getLightEngine();
		Rule rule = type.rule;

		ChunkPos chunkPos = chunk.getPos();

		Heightmap heightmapSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE);
		Heightmap heightmapMotionBlocking = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING);

		// this is the total area over which we need to increase the tide
		BlockPos chunkMin = new BlockPos(
				Math.max(minX, chunkPos.getMinBlockX()),
				fromY,
				Math.max(minZ, chunkPos.getMinBlockZ())
		);
		BlockPos chunkMax = new BlockPos(
				Math.min(maxX, chunkPos.getMaxBlockX()),
				toY,
				Math.min(maxZ, chunkPos.getMaxBlockZ())
		);

		// Shouldn't happen, but we shouldn't try to fill tide outside the given area
		if (chunkMin.getX() > chunkMax.getX() || chunkMin.getZ() > chunkMax.getZ()) {
			return 0;
		}

		long updatedBlocks = 0;

		int fromSection = SectionPos.blockToSectionCoord(fromY);
		int toSection = SectionPos.blockToSectionCoord(toY);

		// iterate through all the sections that need to be changed
		for (int sectionY = fromSection; sectionY <= toSection; sectionY++) {
			LevelChunkSection section = chunk.getSection(level.getSectionIndexFromSectionY(sectionY));
			int minSectionY = SectionPos.sectionToBlockCoord(sectionY);
			int maxSectionY = minSectionY + SectionPos.SECTION_SIZE - 1;

			// Calculate start/end within the current section
			BlockPos sectionMin = new BlockPos(chunkMin.getX(), Math.max(chunkMin.getY(), minSectionY), chunkMin.getZ());
			BlockPos sectionMax = new BlockPos(chunkMax.getX(), Math.min(chunkMax.getY(), maxSectionY), chunkMax.getZ());

			// Don't actually trigger light updates, but make sure the light engine has the information it needs if a block update does happen
			if (section.hasOnlyAir()) {
				lightEngine.updateSectionStatus(SectionPos.of(chunkPos.x(), sectionY, chunkPos.z()), false);
				level.getChunkSource().onSectionEmptinessChanged(chunkPos.x(), sectionY, chunkPos.z(), false);
			}

			boolean changed = false;

			for (BlockPos worldPos : BlockPos.betweenClosed(sectionMin, sectionMax)) {
				int worldY = worldPos.getY();
				int localX = SectionPos.sectionRelative(worldPos.getX());
				int localY = SectionPos.sectionRelative(worldY);
				int localZ = SectionPos.sectionRelative(worldPos.getZ());

				BlockState existingBlock = section.getBlockState(localX, localY, localZ);

				BlockState newBlock = rule.apply(existingBlock, worldY, fromY);
				if (newBlock == existingBlock) {
					continue;
				}

				if (existingBlock.getBlock() != Blocks.BAMBOO) {
					section.setBlockState(localX, localY, localZ, newBlock);
				} else {
					level.setBlock(worldPos, newBlock, Block.UPDATE_INVISIBLE | Block.UPDATE_CLIENTS);
				}

				heightmapSurface.update(localX, worldY, localZ, newBlock);
				heightmapMotionBlocking.update(localX, worldY, localZ, newBlock);

				updatedBlocks++;
				changed = true;
			}

			if (changed && level.isClientSide()) {
				markSectionForRerender(chunkPos.x(), sectionY, chunkPos.z());
			}
		}

		if (updatedBlocks > 0 && !level.isClientSide()) {
			// Make sure this chunk gets saved
			chunk.markUnsaved();
		}

		return updatedBlocks;
	}

	private static boolean is(BlockState state, DeferredHolder<Block, Block> block) {
		// If the block doesn't exist, calling is() will throw
		if (!block.isBound()) {
			return false;
		}
		return state.is(block);
	}

	private static void markSectionForRerender(int sectionX, int sectionY, int sectionZ) {
		final @Nullable ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			return;
		}
		level.setSectionDirtyWithNeighbors(sectionX, sectionY, sectionZ);
	}

	public interface Rule {
		BlockState apply(BlockState state, int y, int fromLevel);
	}

	public record WaterRule() implements Rule {
		private static final BlockState WATER = Blocks.WATER.defaultBlockState();
		private static final DeferredHolder<Block, Block> WATER_BARRIER = DeferredHolder.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("ltextras", "water_barrier"));
		private static final DeferredHolder<Block, Block> SAND_LAYER = DeferredHolder.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("weather2", "sand_layer"));

		@Override
		public BlockState apply(BlockState state, int y, int fromLevel) {
			if (y <= fromLevel) {
				return mapBlockBelowWater(state);
			} else {
				return mapBlockRisingWater(state);
			}
		}

		public static BlockState mapBlockRisingWater(BlockState state) {
			Block block = state.getBlock();

			if (state.isAir() || !state.blocksMotion() || block == Blocks.BAMBOO || is(state, SAND_LAYER)) {
				return WATER;
			}

			if (block instanceof SimpleWaterloggedBlock) {
				// If waterloggable, set the waterloggable property to true
				state = state.setValue(BlockStateProperties.WATERLOGGED, true);
				if (block == Blocks.CAMPFIRE) {
					state = state.setValue(CampfireBlock.LIT, false);
				}
				return state;
			}

			if (block == Blocks.BARRIER) {
				return (WATER_BARRIER.isBound() ? WATER_BARRIER.value() : Blocks.BARRIER).defaultBlockState();
			}

			if (block == Blocks.CONCRETE_POWDER.black()) {
				// adding to the amazing list of hardcoded replacements.. yes!
				return Blocks.CONCRETE_POWDER.black().defaultBlockState();
			}

			return state;
		}

		private static BlockState mapBlockBelowWater(BlockState state) {
			if (state.getBlock() == Blocks.GRASS_BLOCK || state.getBlock() == Blocks.DIRT_PATH) {
				return Blocks.DIRT.defaultBlockState();
			}
			return state;
		}
	}

	public record SimpleRule(BlockState fill) implements Rule {
		@Override
		public BlockState apply(BlockState state, int y, int fromLevel) {
			if (y > fromLevel && state.isAir()) {
				return fill;
			}
			return state;
		}
	}

	public enum Type implements StringRepresentable {
		WATER("water", new WaterRule()),
		LAVA("lava", new SimpleRule(Blocks.LAVA.defaultBlockState())),
		;

		public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);
		public static final StreamCodec<ByteBuf, Type> STREAM_CODEC = ByteBufCodecs.idMapper(
				ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO),
				Enum::ordinal
		);

		private final String name;
		private final Rule rule;

		Type(String name, Rule rule) {
			this.name = name;
			this.rule = rule;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}
}
