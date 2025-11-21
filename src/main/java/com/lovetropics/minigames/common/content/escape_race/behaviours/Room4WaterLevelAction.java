package com.lovetropics.minigames.common.content.escape_race.behaviours;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.instances.RisingFluidBehavior;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticsMap;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.util.FluidFiller;
import com.lovetropics.minigames.common.core.network.FillFluidPacket;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrays;
import it.unimi.dsi.fastutil.longs.LongComparator;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.PacketDistributor;
import org.checkerframework.checker.units.qual.min;

import java.util.function.Supplier;

public class Room4WaterLevelAction implements IGameBehavior {
	public static final MapCodec<Room4WaterLevelAction> CODEC = MapCodec.unit(Room4WaterLevelAction::new);

	private static final int BASE_WATER_LEVEL_FOR_THIS_MAP = 141;

	private int fluidLevel = BASE_WATER_LEVEL_FOR_THIS_MAP;
	private int targetFluidLevel = fluidLevel; // current target
	private int targetLevel; // moving to
	private int timeout = 10;
	private BlockBox region;
	private ChunkPos minChunk;
	private ChunkPos maxChunk;

	private final LongSet highPriorityUpdates = new LongLinkedOpenHashSet();
	private final LongSet lowPriorityUpdates = new LongLinkedOpenHashSet();
	private final Long2IntMap fluidLevelByChunk = new Long2IntOpenHashMap();

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GamePhaseEvents.START, initiator -> {
			region = game.mapRegions().getOrThrow("flood_area");
			minChunk = new ChunkPos(SectionPos.blockToSectionCoord(region.min().getX()), SectionPos.blockToSectionCoord(region.min().getZ()));
			maxChunk = new ChunkPos(SectionPos.blockToSectionCoord(region.max().getX()), SectionPos.blockToSectionCoord(region.max().getZ()));
		});

		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			GameTeamKey team = targets.asTeams(game).getFirst();
			StatisticsMap statistics = game.statistics().forTeam(team);
			int points = statistics.getInt(StatisticKey.POINTS);
			if (points == 2) {
				targetLevel = 157;
			} else if (points == 5) {
				targetLevel = 163;
			} else if (points == 7) {
				targetLevel = 177;
			}

			System.out.println(points + " -> " + targetLevel);

			return true;
		});

		events.listen(GamePhaseEvents.TICK, () -> {
			if (targetLevel > targetFluidLevel) {
				timeout--;
				if (timeout == 0) {
					timeout = 10;
					targetFluidLevel++;
					System.out.println("!! " + targetFluidLevel);
				}
			}
			tickFluidLevel(game);
			processUpdates(game);
		});
	}

	// Mostly copy&pasted RisingFluidBehavior - Sorry!!

	private void processUpdates(IGamePhase game) {
		if (highPriorityUpdates.isEmpty() && lowPriorityUpdates.isEmpty()) {
			return;
		}

		int count = processUpdateQueue(game, highPriorityUpdates.iterator(), RisingFluidBehavior.HIGH_PRIORITY_BUDGET_PER_TICK);
		if (count <= 0) {
			processUpdateQueue(game, lowPriorityUpdates.iterator(), RisingFluidBehavior.LOW_PRIORITY_BUDGET_PER_TICK);
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
		if (fluidLevel < targetFluidLevel) {
			fluidLevel++;

			boolean close = true;
			for (long chunkPos : collectSortedChunks(game)) {
				if (close) {
					highPriorityUpdates.add(chunkPos);
					int distanceSq = getChunkDistanceSq(game, ChunkPos.getX(chunkPos), ChunkPos.getZ(chunkPos));
					if (distanceSq >= RisingFluidBehavior.HIGH_PRIORITY_DISTANCE_SQ) {
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
		if (lastLevel == 0) {
			lastLevel = BASE_WATER_LEVEL_FOR_THIS_MAP;
		}

		if (targetLevel > lastLevel) {
			BlockPos min = region.min();
			BlockPos max = region.max();
			long count = FluidFiller.fillChunk(FluidFiller.Type.WATER, min.getX(), min.getZ(), max.getX(), max.getZ(), chunk, lastLevel, targetLevel);
			if (count > 0) {
				PacketDistributor.sendToPlayersTrackingChunk(level, chunk.getPos(), new FillFluidPacket(
						FluidFiller.Type.WATER,
						new BlockPos(Math.max(min.getX(), chunkPos.getMinBlockX()), lastLevel, Math.max(min.getZ(), chunkPos.getMinBlockZ())),
						new BlockPos(Math.min(max.getX(), chunkPos.getMaxBlockX()), targetLevel, Math.min(max.getZ(), chunkPos.getMaxBlockZ()))
				));
			}
			return count;
		} else {
			return 0;
		}
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return EscapeRace.ROOM4_WATER_LEVEL;
	}
}
