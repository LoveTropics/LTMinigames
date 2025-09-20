package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameLivingEntityEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressChannel;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressHolder;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressionSpline;
import com.lovetropics.minigames.common.core.game.util.FluidFiller;
import com.lovetropics.minigames.common.core.network.FillFluidPacket;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrays;
import it.unimi.dsi.fastutil.longs.LongComparator;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.function.DoubleSupplier;

public class RisingFluidBehavior implements IGameBehavior {
	public static final MapCodec<RisingFluidBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("region").forGetter(c -> c.regionKey),
			ProgressionSpline.CODEC.fieldOf("fluid_levels").forGetter(c -> c.fluidLevels),
			FluidFiller.Type.CODEC.fieldOf("fill_type").forGetter(b -> b.fillType)
	).apply(i, RisingFluidBehavior::new));

	private static final int HIGH_PRIORITY_BUDGET_PER_TICK = 40;
	private static final int LOW_PRIORITY_BUDGET_PER_TICK = 8;

	private static final int HIGH_PRIORITY_DISTANCE_SQ = 64 * 64;

	private final String regionKey;
	private final ProgressionSpline fluidLevels;
	private final FluidFiller.Type fillType;

	private BlockBox region;
	private DoubleSupplier targetFluidLevel = () -> 0.0;
	private int fluidLevel;

	private ChunkPos minChunk;
	private ChunkPos maxChunk;

	private final LongSet highPriorityUpdates = new LongLinkedOpenHashSet();
	private final LongSet lowPriorityUpdates = new LongLinkedOpenHashSet();
	private final Long2IntMap fluidLevelByChunk = new Long2IntOpenHashMap();

	public RisingFluidBehavior(String regionKey, ProgressionSpline fluidLevels, FluidFiller.Type fillType) {
		this.regionKey = regionKey;
		this.fluidLevels = fluidLevels;
		this.fillType = fillType;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		region = game.mapRegions().getOrThrow(regionKey);
		minChunk = new ChunkPos(SectionPos.blockToSectionCoord(region.min().getX()), SectionPos.blockToSectionCoord(region.min().getZ()));
		maxChunk = new ChunkPos(SectionPos.blockToSectionCoord(region.max().getX()), SectionPos.blockToSectionCoord(region.max().getZ()));

		ProgressHolder progression = ProgressChannel.MAIN.getOrThrow(game);
		targetFluidLevel = fluidLevels.resolve(progression);

		events.listen(GamePhaseEvents.START, () -> {
			fluidLevel = Mth.floor(targetFluidLevel.getAsDouble());
			fluidLevelByChunk.defaultReturnValue(fluidLevel);
		});

		if (fillType == FluidFiller.Type.WATER) {
			events.listen(GameLivingEntityEvents.TICK, this::onLivingUpdateInWater);
		}
		events.listen(GamePhaseEvents.TICK, () -> tick(game));
	}

	private void onLivingUpdateInWater(LivingEntity entity) {
		// NOTE: DO NOT REMOVE THIS CHECK, CAUSES FISH TO DIE AND SPAWN ITEMS ON DEATH
		// FISH WILL KEEP SPAWNING, DYING AND COMPLETELY SLOW THE SERVER TO A CRAWL
		if (!entity.canBreatheUnderwater()) {
			if (entity.getY() <= fluidLevel + 1 && entity.isInWater() && entity.tickCount % 40 == 0) {
				entity.hurt(entity.damageSources().drown(), 2.0F);
			}
		}
	}

	private void tick(IGamePhase game) {
		tickFluidLevel(game);
		processUpdates(game);
		if (fillType == FluidFiller.Type.WATER) {
			spawnWarningParticles(game);
		}
	}

	private void spawnWarningParticles(IGamePhase game) {
		ServerLevel world = game.level();
		RandomSource random = world.random;
		if (random.nextInt(3) != 0) {
			return;
		}

		BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

		for (ServerPlayer player : game.participants()) {
			// only attempt to spawn particles if the player is near the water surface
			if (Math.abs(player.getY() - fluidLevel) > 5) {
				continue;
			}

			int particleX = Mth.floor(player.getX()) - random.nextInt(5) + random.nextInt(5);
			int particleZ = Mth.floor(player.getZ()) - random.nextInt(5) + random.nextInt(5);
			mutablePos.set(particleX, fluidLevel, particleZ);

			if (!world.isEmptyBlock(mutablePos) && world.isEmptyBlock(mutablePos.move(Direction.UP))) {
				Packet<?> packet = new ClientboundLevelParticlesPacket(ParticleTypes.SPLASH, false, false, particleX, fluidLevel + 1, particleZ, 0.1F, 0.0F, 0.1F, 0.0F, 4);
				player.connection.send(packet);
			}
		}
	}

	private void processUpdates(IGamePhase game) {
		if (highPriorityUpdates.isEmpty() && lowPriorityUpdates.isEmpty()) {
			return;
		}

		int count = processUpdateQueue(game, highPriorityUpdates.iterator(), HIGH_PRIORITY_BUDGET_PER_TICK);
		if (count <= 0) {
			processUpdateQueue(game, lowPriorityUpdates.iterator(), LOW_PRIORITY_BUDGET_PER_TICK);
		}
	}

	private int processUpdateQueue(IGamePhase game, LongIterator iterator, int maxToProcess) {
		ServerLevel level = game.level();
		ServerChunkCache chunkProvider = level.getChunkSource();

		int count = 0;

		while (count < maxToProcess && iterator.hasNext()) {
			long chunkPos = iterator.nextLong();
			int chunkX = ChunkPos.getX(chunkPos);
			int chunkZ = ChunkPos.getZ(chunkPos);

			// we only want to apply updates to loaded chunks
			LevelChunk chunk = chunkProvider.getChunkNow(chunkX, chunkZ);
			if (chunk == null) {
				continue;
			}

			iterator.remove();
			increaseInChunk(level, chunk);
			count++;
		}

		return count;
	}

	private void tickFluidLevel(final IGamePhase game) {
		int targetFluidLevel = Mth.floor(this.targetFluidLevel.getAsDouble());

		if (fluidLevel < targetFluidLevel) {
			fluidLevel++;

			boolean close = true;
			for (long chunkPos : collectSortedChunks(game)) {
				if (close) {
					highPriorityUpdates.add(chunkPos);
					int distanceSq = getChunkDistanceSq(game, ChunkPos.getX(chunkPos), ChunkPos.getZ(chunkPos));
					if (distanceSq >= HIGH_PRIORITY_DISTANCE_SQ) {
						close = false;
					}
				} else {
					lowPriorityUpdates.add(chunkPos);
				}
			}
		}
	}

	private long[] collectSortedChunks(IGamePhase game) {
		LongComparator distanceComparator = (pos1, pos2) -> Integer.compare(
				getChunkDistanceSq(game, ChunkPos.getX(pos1), ChunkPos.getZ(pos1)),
				getChunkDistanceSq(game, ChunkPos.getX(pos2), ChunkPos.getZ(pos2))
		);

		int sizeX = maxChunk.x - minChunk.x + 1;
		int sizeZ = maxChunk.z - minChunk.z + 1;

		long[] chunks = new long[sizeX * sizeZ];

		int i = 0;
		for (int z = minChunk.z; z <= maxChunk.z; z++) {
			for (int x = minChunk.x; x <= maxChunk.x; x++) {
				chunks[i++] = ChunkPos.asLong(x, z);
			}
		}

		LongArrays.unstableSort(chunks, distanceComparator);

		return chunks;
	}

	private int getChunkDistanceSq(IGamePhase game, int x, int z) {
		int minDistanceSq = Integer.MAX_VALUE;
		int centerX = SectionPos.sectionToBlockCoord(x) + SectionPos.SECTION_HALF_SIZE;
		int centerZ = SectionPos.sectionToBlockCoord(z) + SectionPos.SECTION_HALF_SIZE;
		for (ServerPlayer player : game.allPlayers()) {
			int dx = player.getBlockX() - centerX;
			int dz = player.getBlockZ() - centerZ;
			int distanceSq = dx * dx + dz * dz;
			if (distanceSq < minDistanceSq) {
				minDistanceSq = distanceSq;
			}
		}
		return minDistanceSq;
	}

	private long increaseInChunk(ServerLevel level, LevelChunk chunk) {
		ChunkPos chunkPos = chunk.getPos();

		int targetLevel = fluidLevel;
		int lastLevel = fluidLevelByChunk.put(chunkPos.toLong(), targetLevel);

		if (targetLevel > lastLevel) {
			BlockPos min = region.min();
			BlockPos max = region.max();
			long count = FluidFiller.fillChunk(fillType, min.getX(), min.getZ(), max.getX(), max.getZ(), chunk, lastLevel, targetLevel);
			if (count > 0) {
				PacketDistributor.sendToPlayersTrackingChunk(level, chunk.getPos(), new FillFluidPacket(
						fillType,
						new BlockPos(Math.max(min.getX(), chunkPos.getMinBlockX()), lastLevel, Math.max(min.getZ(), chunkPos.getMinBlockZ())),
						new BlockPos(Math.min(max.getX(), chunkPos.getMaxBlockX()), targetLevel, Math.min(max.getZ(), chunkPos.getMaxBlockZ()))
				));
			}
			return count;
		} else {
			return 0;
		}
	}
}
