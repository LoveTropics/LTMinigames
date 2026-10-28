package org.lovetropics.games.common.core.game.behavior.instances.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.world.level.ChunkPos;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameWorldEvents;
import org.lovetropics.games.common.core.game.region.RegionQuery;

public record ForceLoadRegionBehavior(
		RegionQuery region
) implements IGameBehavior {
	public static final MapCodec<ForceLoadRegionBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			RegionQuery.CODEC.fieldOf("region").forGetter(c -> c.region)
	).apply(i, ForceLoadRegionBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GameWorldEvents.CREATE_LEVEL, level -> {
			LongOpenHashSet chunks = LongOpenHashSet.toSet(region.allIn(game, level).stream()
					.flatMapToLong(region -> region.chunks().longStream()));

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
}
