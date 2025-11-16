package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SetBlocksAction implements IGameBehavior {
	public static final MapCodec<SetBlocksAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			BlockPredicate.CODEC.optionalFieldOf("replace").forGetter(c -> Optional.ofNullable(c.replace)),
			MoreCodecs.BLOCK_STATE_PROVIDER.fieldOf("set").forGetter(c -> c.set),
			MoreCodecs.arrayOrUnit(Codec.STRING, String[]::new).optionalFieldOf("region", new String[0]).forGetter(c -> c.regionKeys),
			Codec.BOOL.optionalFieldOf("notify_neighbors", true).forGetter(c -> c.notifyNeighbors)
	).apply(i, SetBlocksAction::new));

	private final @Nullable BlockPredicate replace;
	private final BlockStateProvider set;

	private final String[] regionKeys;

	private final boolean notifyNeighbors;

	private SetBlocksAction(Optional<BlockPredicate> replace, BlockStateProvider set, String[] regionKeys, boolean notifyNeighbors) {
		this(replace.orElse(null), set, regionKeys, notifyNeighbors);
	}

	public SetBlocksAction(BlockPredicate replace, BlockStateProvider set, String[] regionKeys, boolean notifyNeighbors) {
		this.replace = replace;
		this.set = set;
		this.regionKeys = regionKeys;
		this.notifyNeighbors = notifyNeighbors;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		List<BlockBox> regions = new ArrayList<>();
		for (String regionKey : regionKeys) {
			regions.addAll(game.mapRegions().get(regionKey));
		}

		if (regions.isEmpty()) {
			throw new GameException(Component.literal("Regions not specified for block set behavior with a set time!"));
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
		RandomSource random = world.random;

		loadRegionChunks(region, world);

		int flags = Block.UPDATE_ALL;
		if (!notifyNeighbors) {
			// the constant name is inverted
			flags |= Block.UPDATE_KNOWN_SHAPE;
		}

		for (BlockPos pos : region) {
			if (replace == null || replace.matches(world, pos)) {
				BlockState state = set.getState(random, pos);
				world.setBlock(pos, state, flags);
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
