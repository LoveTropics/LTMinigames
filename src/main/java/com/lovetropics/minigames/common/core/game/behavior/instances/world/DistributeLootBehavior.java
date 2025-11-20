package com.lovetropics.minigames.common.core.game.behavior.instances.world;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameWorldEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;

public record DistributeLootBehavior(
		String region,
		BlockEntityType<?> blockEntityType,
		ItemStack item,
		int count
) implements IGameBehavior {
	public static final MapCodec<DistributeLootBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("region").forGetter(DistributeLootBehavior::region),
			BuiltInRegistries.BLOCK_ENTITY_TYPE.byNameCodec().fieldOf("block_entity_type").forGetter(DistributeLootBehavior::blockEntityType),
			MoreCodecs.ITEM_STACK.fieldOf("item").forGetter(DistributeLootBehavior::item),
			ExtraCodecs.POSITIVE_INT.fieldOf("count").forGetter(DistributeLootBehavior::count)
	).apply(i, DistributeLootBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		BlockBox region = game.mapRegions().getOrThrow(this.region);

		LongSet pendingChunks = new LongOpenHashSet(region.asChunks());
		pendingChunks.forEach(chunkKey ->
				game.level().getChunkSource().updateChunkForced(new ChunkPos(chunkKey), true)
		);

		events.listen(GameWorldEvents.CHUNK_LOAD, chunk -> {
			ChunkPos chunkPos = chunk.getPos();
			if (pendingChunks.remove(chunkPos.toLong()) && pendingChunks.isEmpty()) {
				onRegionFullyLoaded(game, region);
			}
		});
	}

	private void onRegionFullyLoaded(IGamePhase game, BlockBox box) {
		List<Container> containers = new ArrayList<>();
		box.asChunks().forEach(chunkKey -> {
			LevelChunk chunk = game.level().getChunk(ChunkPos.getX(chunkKey), ChunkPos.getZ(chunkKey));
			for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
				BlockPos pos = blockEntity.getBlockPos();
				if (!box.contains(pos.getX(), box.min().getY(), pos.getZ())) {
					return;
				}
				if (blockEntity instanceof Container container && blockEntity.getType().equals(blockEntityType)) {
					containers.add(container);
				}
			}
			game.level().getChunkSource().updateChunkForced(chunk.getPos(), false);
		});

		RandomSource random = game.random();
		Util.shuffle(containers, random);

		// TODO: This is very simplistic for now - should do something like loot table spreading items, and randomise more per container
		int[] countPerContainer = new int[containers.size()];
		int itemsRemaining = count;
		for (int i = 0; i < containers.size(); i++) {
			int containersRemaining = containers.size() - i;
			int itemsInContainer = itemsRemaining / containersRemaining;
			countPerContainer[i] = itemsInContainer;
			itemsRemaining -= itemsInContainer;
		}

		for (int i = 0; i < containers.size(); i++) {
			containers.get(i).setItem(0, item.copyWithCount(countPerContainer[i]));
		}
	}
}
