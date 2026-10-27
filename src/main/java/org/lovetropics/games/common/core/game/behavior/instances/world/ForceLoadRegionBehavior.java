package org.lovetropics.games.common.core.game.behavior.instances.world;

import com.lovetropics.lib.BlockBox;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.world.level.ChunkPos;
import org.lovetropics.games.common.core.game.GameRegions;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameWorldEvents;

public record ForceLoadRegionBehavior(
		String regionKey
) implements IGameBehavior {
	public static final MapCodec<ForceLoadRegionBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("region").forGetter(c -> c.regionKey)
	).apply(i, ForceLoadRegionBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GameWorldEvents.CREATE_LEVEL, (level, levelRegions) -> {
			LongSet chunks = collectChunks(levelRegions);

			LongIterator iterator = chunks.iterator();
			while (iterator.hasNext()) {
				long chunkKey = iterator.nextLong();
				level.getChunkSource().updateChunkForced(ChunkPos.unpack(chunkKey), true);
			}

			iterator = chunks.iterator();
			while (iterator.hasNext()) {
				long chunkKey = iterator.nextLong();
				level.getChunkSource().getChunk(ChunkPos.getX(chunkKey), ChunkPos.getZ(chunkKey), true);
			}
		});
	}

	private LongSet collectChunks(GameRegions regions) {
		LongSet chunks = new LongOpenHashSet();
		for (BlockBox region : regions.get(regionKey)) {
			chunks.addAll(region.asChunks());
		}
		return chunks;
	}
}
