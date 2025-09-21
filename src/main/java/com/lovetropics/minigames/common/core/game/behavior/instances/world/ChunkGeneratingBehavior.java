package com.lovetropics.minigames.common.core.game.behavior.instances.world;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameWorldEvents;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

public abstract class ChunkGeneratingBehavior implements IGameBehavior {
	private final LongSet generatedChunks = new LongOpenHashSet();

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GameWorldEvents.CHUNK_LOAD, chunk -> {
			if (chunk instanceof LevelChunk levelChunk && generatedChunks.add(chunk.getPos().toLong())) {
				generateChunk(game, (ServerLevel) levelChunk.getLevel(), levelChunk);
			}
		});
	}

	protected abstract void generateChunk(IGamePhase game, ServerLevel world, LevelChunk chunk);
}
