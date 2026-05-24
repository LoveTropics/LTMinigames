package com.lovetropics.minigames.common.core.game.behavior.instances.world;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.ChunkPos;

import java.util.Collection;

public final class ForceLoadRegionBehavior implements IGameBehavior {
	public static final MapCodec<ForceLoadRegionBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("region").forGetter(c -> c.regionKey)
	).apply(i, ForceLoadRegionBehavior::new));

	private final String regionKey;
	private LongSet acquiredChunks;

	public ForceLoadRegionBehavior(String regionKey) {
		this.regionKey = regionKey;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		acquiredChunks = acquireChunks(game);
		events.listen(GamePhaseEvents.STOP, reason -> onStop(game));
	}

	private void onStop(IGamePhase game) {
		ServerChunkCache chunkProvider = game.level().getChunkSource();

		LongIterator iterator = acquiredChunks.iterator();
		while (iterator.hasNext()) {
			long chunkKey = iterator.nextLong();
			chunkProvider.updateChunkForced(ChunkPos.unpack(chunkKey), false);
		}
	}

	private LongSet acquireChunks(IGamePhase game) {
		ServerChunkCache chunkProvider = game.level().getChunkSource();

		LongSet chunks = collectChunks(game);

		LongIterator iterator = chunks.iterator();
		while (iterator.hasNext()) {
			long chunkKey = iterator.nextLong();
			chunkProvider.updateChunkForced(ChunkPos.unpack(chunkKey), true);
		}

		iterator = chunks.iterator();
		while (iterator.hasNext()) {
			long chunkKey = iterator.nextLong();
			chunkProvider.getChunk(ChunkPos.getX(chunkKey), ChunkPos.getZ(chunkKey), true);
		}

		return chunks;
	}

	private LongSet collectChunks(IGamePhase game) {
		LongSet chunks = new LongOpenHashSet();

		Collection<BlockBox> regions = game.mapRegions().get(regionKey);
		for (BlockBox region : regions) {
			chunks.addAll(region.asChunks());
		}

		return chunks;
	}
}
