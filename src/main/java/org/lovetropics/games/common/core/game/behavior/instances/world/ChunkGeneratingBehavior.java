package org.lovetropics.games.common.core.game.behavior.instances.world;

import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameWorldEvents;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

public abstract class ChunkGeneratingBehavior implements IGameBehavior {
	private final LongSet generatedChunks = new LongOpenHashSet();

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GameWorldEvents.CHUNK_LOAD, (level, chunk) -> {
			if (chunk instanceof LevelChunk levelChunk && generatedChunks.add(chunk.getPos().pack())) {
				generateChunk(game, level, levelChunk);
			}
		});
	}

	protected abstract void generateChunk(IGamePhase game, ServerLevel level, LevelChunk chunk);
}
