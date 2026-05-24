package com.lovetropics.minigames.common.core.game.behavior.instances.world;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.util.EntityTemplate;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

public final class GenerateEntitiesBehavior extends ChunkGeneratingBehavior {
	public static final MapCodec<GenerateEntitiesBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			EntityTemplate.CODEC.fieldOf("entity").forGetter(c -> c.entity),
			Codec.INT.optionalFieldOf("min_per_chunk", 0).forGetter(c -> c.minPerChunk),
			Codec.INT.optionalFieldOf("max_per_chunk", 1).forGetter(c -> c.maxPerChunk)
	).apply(i, GenerateEntitiesBehavior::new));

	private final EntityTemplate entity;
	private final int minPerChunk;
	private final int maxPerChunk;

	public GenerateEntitiesBehavior(EntityTemplate entity, int minPerChunk, int maxPerChunk) {
		this.entity = entity;
		this.minPerChunk = minPerChunk;
		this.maxPerChunk = maxPerChunk;
	}

	@Override
	protected void generateChunk(IGamePhase game, ServerLevel level, LevelChunk chunk) {
		ChunkPos chunkPos = chunk.getPos();
		int minX = chunkPos.getMinBlockX();
		int minZ = chunkPos.getMinBlockZ();

		RandomSource random = level.getRandom();

		int count = random.nextInt(maxPerChunk - minPerChunk + 1) + minPerChunk;
		for (int i = 0; i < count; i++) {
			int x = minX + random.nextInt(16);
			int z = minZ + random.nextInt(16);

			BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
			float yRot = Mth.wrapDegrees(random.nextFloat() * 360.0f);
			entity.spawn(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yRot, 0.0f, EntitySpawnReason.CHUNK_GENERATION);
		}
	}
}
