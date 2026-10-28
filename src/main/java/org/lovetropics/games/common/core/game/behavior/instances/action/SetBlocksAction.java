package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.lovetropics.lib.codec.MoreCodecs;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongIterator;
import net.minecraft.advancements.predicates.BlockPredicate;
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
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.core.game.region.Region;
import org.lovetropics.games.common.core.game.region.RegionQuery;
import org.slf4j.Logger;

import java.util.Optional;

public final class SetBlocksAction implements IGameBehavior {
	private static final Logger LOGGER = LogUtils.getLogger();
	public static final MapCodec<SetBlocksAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			BlockPredicate.CODEC.optionalFieldOf("replace").forGetter(c -> Optional.ofNullable(c.replace)),
			MoreCodecs.BLOCK_STATE_PROVIDER.fieldOf("set").forGetter(c -> c.set),
			RegionQuery.CODEC.optionalFieldOf("region", RegionQuery.EMPTY).forGetter(c -> c.regions),
			Codec.BOOL.optionalFieldOf("notify_neighbors", true).forGetter(c -> c.notifyNeighbors),
			CompoundTag.CODEC.optionalFieldOf("block_entity_data").forGetter(c -> c.blockEntityData)
	).apply(i, SetBlocksAction::new));

	private final @Nullable BlockPredicate replace;
	private final BlockStateProvider set;

	private final RegionQuery regions;

	private final boolean notifyNeighbors;

	private final Optional<CompoundTag> blockEntityData;

	private SetBlocksAction(Optional<BlockPredicate> replace, BlockStateProvider set, RegionQuery regions, boolean notifyNeighbors, Optional<CompoundTag> blockEntityData) {
		this(replace.orElse(null), set, regions, notifyNeighbors, blockEntityData);
	}

	public SetBlocksAction(BlockPredicate replace, BlockStateProvider set, RegionQuery regions, boolean notifyNeighbors, Optional<CompoundTag> blockEntityData) {
		this.replace = replace;
		this.set = set;
		this.regions = regions;
		this.notifyNeighbors = notifyNeighbors;
		this.blockEntityData = blockEntityData;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			setInRegion(game);
			return true;
		});
	}

	private void setInRegion(IGamePhase game) {
		BlockPredicate replace = this.replace;
		BlockStateProvider set = this.set;
		RandomSource random = game.random();

		loadRegionChunks(game);

		int flags = notifyNeighbors ? Block.UPDATE_ALL : Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE;

		regions.forEachBlock(game, (level, pos) -> {
			if (replace == null || replace.matches(level, pos)) {
				BlockState state = set.getState(level, random, pos);
				level.setBlock(pos, state, flags);
				blockEntityData.ifPresent(tag -> {
					if (level.getBlockEntity(pos) instanceof BlockEntity be) {
						try (ProblemReporter.ScopedCollector scopedCollector = new ProblemReporter.ScopedCollector(be.problemPath(), LOGGER)) {
							be.loadWithComponents(TagValueInput.create(scopedCollector, level.registryAccess(), tag));
						}
					}
				});
			}
		});
	}

	private void loadRegionChunks(IGamePhase game) {
		for (Region region : regions.all(game)) {
			ServerLevel level = region.level();
			LongIterator chunkIterator = region.chunks().iterator();
			while (chunkIterator.hasNext()) {
				long chunkPos = chunkIterator.nextLong();
				level.getChunk(ChunkPos.getX(chunkPos), ChunkPos.getZ(chunkPos));
			}
		}
	}
}
