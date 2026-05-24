package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.advancements.criterion.BlockPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.storage.TagValueInput;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SetBlocksAction implements IGameBehavior {
	private static final Logger LOGGER = LogUtils.getLogger();
	public static final MapCodec<SetBlocksAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			BlockPredicate.CODEC.optionalFieldOf("replace").forGetter(c -> Optional.ofNullable(c.replace)),
			MoreCodecs.BLOCK_STATE_PROVIDER.fieldOf("set").forGetter(c -> c.set),
			MoreCodecs.arrayOrUnit(Codec.STRING, String[]::new).optionalFieldOf("region", new String[0]).forGetter(c -> c.regionKeys),
			Codec.BOOL.optionalFieldOf("notify_neighbors", true).forGetter(c -> c.notifyNeighbors),
			CompoundTag.CODEC.optionalFieldOf("block_entity_data").forGetter(c -> c.blockEntityData)
	).apply(i, SetBlocksAction::new));

	private final @Nullable BlockPredicate replace;
	private final BlockStateProvider set;

	private final String[] regionKeys;

	private final boolean notifyNeighbors;

	private final Optional<CompoundTag> blockEntityData;

	private SetBlocksAction(Optional<BlockPredicate> replace, BlockStateProvider set, String[] regionKeys, boolean notifyNeighbors, Optional<CompoundTag> blockEntityData) {
		this(replace.orElse(null), set, regionKeys, notifyNeighbors, blockEntityData);
	}

	public SetBlocksAction(BlockPredicate replace, BlockStateProvider set, String[] regionKeys, boolean notifyNeighbors, Optional<CompoundTag> blockEntityData) {
		this.replace = replace;
		this.set = set;
		this.regionKeys = regionKeys;
		this.notifyNeighbors = notifyNeighbors;
		this.blockEntityData = blockEntityData;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		List<BlockBox> regions = new ArrayList<>();
		for (String regionKey : regionKeys) {
			regions.addAll(game.mapRegions().getAllOrThrow(regionKey));
		}

		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			for (BlockBox region : regions) {
				setInRegion(game, region);
			}
			return true;
		});
	}

	private void setInRegion(IGamePhase game, BlockBox region) {
		ServerLevel world = game.level();
		BlockPredicate replace = this.replace;
		BlockStateProvider set = this.set;
		RandomSource random = world.getRandom();

		loadRegionChunks(region, world);

		int flags = Block.UPDATE_ALL;
		if (!notifyNeighbors) {
			// the constant name is inverted
			flags |= Block.UPDATE_KNOWN_SHAPE;
		}

		for (BlockPos pos : region) {
			if (replace == null || replace.matches(world, pos)) {
				BlockState state = set.getState(world, random, pos);
				world.setBlock(pos, state, flags);
				blockEntityData.ifPresent(tag -> {
					if (world.getBlockEntity(pos) instanceof BlockEntity be) {
						try (ProblemReporter.ScopedCollector scopedCollector = new ProblemReporter.ScopedCollector(be.problemPath(), LOGGER)) {
							be.loadWithComponents(TagValueInput.create(scopedCollector, world.registryAccess(), tag));
						}
					}
				});
			}
		}
	}

	private void loadRegionChunks(BlockBox region, ServerLevel world) {
		LongSet chunks = region.asChunks();
		LongIterator chunkIterator = chunks.iterator();
		while (chunkIterator.hasNext()) {
			long chunkPos = chunkIterator.nextLong();
			world.getChunk(ChunkPos.getX(chunkPos), ChunkPos.getZ(chunkPos));
		}
	}
}
