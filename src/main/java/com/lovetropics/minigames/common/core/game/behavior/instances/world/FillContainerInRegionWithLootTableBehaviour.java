package com.lovetropics.minigames.common.core.game.behavior.instances.world;

import com.lovetropics.lib.BlockBox;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.Collection;

public record FillContainerInRegionWithLootTableBehaviour(
		String region,
		ResourceKey<LootTable> lootTable
) implements IGameBehavior {

	public static final MapCodec<FillContainerInRegionWithLootTableBehaviour> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("region").forGetter(FillContainerInRegionWithLootTableBehaviour::region),
			ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("loot_table").forGetter(FillContainerInRegionWithLootTableBehaviour::lootTable)
	).apply(i, FillContainerInRegionWithLootTableBehaviour::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		Collection<BlockBox> containerRegions = game.mapRegions().getAll(region);

		LongSet pendingChunks = new LongOpenHashSet();
		containerRegions.forEach(box -> {
			pendingChunks.addAll(box.asChunks());
		});
		pendingChunks.forEach(chunkKey ->
				game.level().getChunkSource().updateChunkForced(new ChunkPos(chunkKey), true)
		);

		events.listen(GameWorldEvents.CHUNK_LOAD, chunk -> {
			ChunkPos chunkPos = chunk.getPos();
			if (pendingChunks.remove(chunkPos.toLong()) && pendingChunks.isEmpty()) {
				onRegionFullyLoaded(game, containerRegions);
			}
		});
	}


	private void onRegionFullyLoaded(IGamePhase game, Collection<BlockBox> boxes) {
		// Rushes dodgy code for filling the chests
		boxes.forEach(containerRegion -> {
			for (long chunk : containerRegion.asChunks()) {
				LevelChunk levelChunk = game.level().getChunk(ChunkPos.getX(chunk), ChunkPos.getZ(chunk));
				for (BlockPos pos : levelChunk.getBlockEntitiesPos()) {
					if(containerRegion.contains(pos)) {
						if (levelChunk.getBlockEntity(pos) instanceof RandomizableContainer container) {
							container.setLootTable(lootTable, game.level().random.nextLong());
						}
					}
				}
			}
		});
	}
}
