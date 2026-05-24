package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant.placement;

import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event.BbPlantEvents;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.PlantCoverage;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.PlantPlacement;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.util.BlockStatePredicate;
import com.lovetropics.minigames.common.util.world.DelegatingWorldGenLevel;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import javax.annotation.Nullable;
import java.util.function.Predicate;

public record PlaceFeaturePlantBehavior(Holder<ConfiguredFeature<?, ?>> feature, BlockStatePredicate blocks) implements IGameBehavior {
	public static final MapCodec<PlaceFeaturePlantBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ConfiguredFeature.CODEC.fieldOf("feature").forGetter(c -> c.feature),
			BlockStatePredicate.CODEC.fieldOf("blocks").forGetter(c -> c.blocks)
	).apply(i, PlaceFeaturePlantBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(BbPlantEvents.PLACE, (player, plot, pos) -> {
			ServerLevel world = game.level();
			ConfiguredFeature<?, ?> tree = feature.value();
			Long2ObjectMap<BlockState> changedBlocks = generateFeature(world, pos, tree);
			if (changedBlocks != null) {
				return buildPlacement(pos, changedBlocks);
			} else {
				return null;
			}
		});
	}

	@Nullable
	private Long2ObjectMap<BlockState> generateFeature(ServerLevel world, BlockPos pos, ConfiguredFeature<?, ?> feature) {
		BlockCapturingWorld capturingWorld = new BlockCapturingWorld(world, blocks);

		ChunkGenerator chunkGenerator = world.getChunkSource().getGenerator();
		if (feature.place(capturingWorld, chunkGenerator, world.getRandom(), pos)) {
			return capturingWorld.getCapturedBlocks();
		} else {
			return null;
		}
	}

	@Nullable
	private PlantPlacement buildPlacement(BlockPos origin, Long2ObjectMap<BlockState> blocks) {
		LongSet coverage = new LongOpenHashSet();
		LongSet decorationCoverage = new LongOpenHashSet();

		for (Long2ObjectMap.Entry<BlockState> entry : Long2ObjectMaps.fastIterable(blocks)) {
			long pos = entry.getLongKey();
			BlockState state = entry.getValue();
			if (!isDecorationBlock(state)) {
				coverage.add(pos);
			} else {
				decorationCoverage.add(pos);
			}
		}

		if (coverage.isEmpty()) {
			return null;
		}

		PlantPlacement placement = new PlantPlacement()
				.covers(PlantCoverage.of(coverage, origin));

		if (!decorationCoverage.isEmpty()) {
			placement.decorationCovers(PlantCoverage.of(decorationCoverage, origin));
		}

		return placement.places((world, finalCoverage) -> {
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			for (Long2ObjectMap.Entry<BlockState> entry : Long2ObjectMaps.fastIterable(blocks)) {
				pos.set(entry.getLongKey());
				if (finalCoverage.covers(pos)) {
					BlockState state = entry.getValue();
					world.setBlock(pos, state, Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE);
				}
			}
			return true;
		});
	}

	private static boolean isDecorationBlock(BlockState state) {
		return !state.is(BlockTags.LOGS);
	}

	static class BlockCapturingWorld extends DelegatingWorldGenLevel {
		private final Long2ObjectMap<BlockState> simulatedBlocks = new Long2ObjectOpenHashMap<>();
		private final Long2ObjectMap<BlockState> capturedBlocks = new Long2ObjectOpenHashMap<>();

		private final Predicate<BlockState> filter;

		BlockCapturingWorld(WorldGenLevel parent, Predicate<BlockState> filter) {
			super(parent);
			this.filter = filter;
		}

		Long2ObjectMap<BlockState> getCapturedBlocks() {
			return capturedBlocks;
		}

		@Override
		public boolean setBlock(BlockPos pos, BlockState state, int flags, int recursionLeft) {
			BlockState oldState = getBlockState(pos);
			if (oldState == state) {
				return false;
			}

			long posKey = pos.asLong();
			if (!state.equals(super.getBlockState(pos))) {
				simulatedBlocks.put(posKey, state);
				if (filter.test(state)) {
					capturedBlocks.put(posKey, state);
				} else {
					capturedBlocks.remove(posKey);
				}
			} else {
				simulatedBlocks.remove(posKey);
				capturedBlocks.remove(posKey);
			}

			return true;
		}

		@Override
		public BlockState getBlockState(BlockPos pos) {
			BlockState changedBlock = simulatedBlocks.get(pos.asLong());
			if (changedBlock != null) {
				return changedBlock;
			} else {
				return super.getBlockState(pos);
			}
		}
	}
}
