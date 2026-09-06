package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.util.EntityTemplate;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

public class SpawnEntitiesAroundPlayersAction implements IGameBehavior {
	public static final MapCodec<SpawnEntitiesAroundPlayersAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			EntityTemplate.CODEC.fieldOf("entity").forGetter(c -> c.entity),
			Codec.INT.optionalFieldOf("entity_count_per_player", 1).forGetter(c -> c.entityCountPerPlayer),
			Codec.INT.optionalFieldOf("spawn_distance_min", 10).forGetter(c -> c.spawnDistanceMin),
			Codec.INT.optionalFieldOf("spawn_distance_max", 20).forGetter(c -> c.spawnDistanceMax),
			Codec.INT.optionalFieldOf("spawn_range_y", 10).forGetter(c -> c.spawnRangeY),
			Codec.INT.optionalFieldOf("spawn_try_rate", 10).forGetter(c -> c.spawnsPerTick),
			Codec.INT.optionalFieldOf("max_entity_count", Integer.MAX_VALUE).forGetter(c -> c.maxEntityCount)
	).apply(i, SpawnEntitiesAroundPlayersAction::new));

	private final EntityTemplate entity;
	private final int entityCountPerPlayer;
	private final int spawnDistanceMin;
	private final int spawnDistanceMax;
	private final int spawnRangeY;
	private final int spawnsPerTick;
	private final int maxEntityCount;
	private int remainingEntityCount;
	private final Object2IntMap<ServerPlayer> playerToAmountToSpawn = new Object2IntOpenHashMap<>();

	public SpawnEntitiesAroundPlayersAction(final EntityTemplate entity, final int entityCount, final int spawnDistanceMin, final int spawnDistanceMax, final int spawnRangeY, final int spawnsPerTick, final int maxEntityCount) {
		this.entity = entity;
		entityCountPerPlayer = entityCount;
		this.spawnDistanceMin = spawnDistanceMin;
		this.spawnDistanceMax = spawnDistanceMax;
		this.spawnRangeY = spawnRangeY;
		this.spawnsPerTick = spawnsPerTick;
		this.maxEntityCount = maxEntityCount;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			List<ServerPlayer> players = targets.asPlayers(game);
			if (players.isEmpty()) {
				return false;
			}
			remainingEntityCount = maxEntityCount;
			for (ServerPlayer player : players) {
				playerToAmountToSpawn.put(player, entityCountPerPlayer);
			}
			return true;
		});
		events.listen(GamePhaseEvents.TICK, () -> tick(game));
	}

	private void tick(IGamePhase game) {
		Iterator<Object2IntMap.Entry<ServerPlayer>> it = playerToAmountToSpawn.object2IntEntrySet().iterator();
		while (it.hasNext()) {
			Object2IntMap.Entry<ServerPlayer> entry = it.next();

			if (!entry.getKey().isAlive()) {
				it.remove();
			} else {

				BlockPos pos = getSpawnableRandomPositionNear(game, entry.getKey().blockPosition(), spawnDistanceMin, spawnDistanceMax, spawnsPerTick, spawnRangeY);

				if (pos != null) {
					entry.setValue(entry.getIntValue() - 1);
					if (entry.getIntValue() <= 0) {
						it.remove();
					}

					entity.spawn(game.level(), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0f, 0.0f);

					if (--remainingEntityCount == 0) {
						playerToAmountToSpawn.clear();
						return;
					}
				}
			}
		}
	}

	/// Tries to return a random spawnable position within the set distances up to a certain amount of attempts
	///
	/// @return null if it fails, otherwise a real position
	@Nullable
	public BlockPos getSpawnableRandomPositionNear(final IGamePhase game, BlockPos pos, int minDist, int maxDist, int loopAttempts, int yRange) {
		for (int i = 0; i < loopAttempts; i++) {
			BlockPos posTry = pos.offset(game.level().getRandom().nextInt(maxDist * 2) - maxDist,
					game.level().getRandom().nextInt(yRange * 2) - yRange,
					game.level().getRandom().nextInt(maxDist * 2) - maxDist);

			if (pos.distSqr(posTry) >= minDist * minDist && isSpawnablePosition(game, posTry)) {
				return posTry;
			}
		}
		return null;
	}

	/// Quick and dirty check for 2 high air with non air block under it
	/// - also checks that it isnt water under it
	public boolean isSpawnablePosition(final IGamePhase game, BlockPos pos) {
		ServerLevel world = game.level();
		return !world.isEmptyBlock(pos.offset(0, -1, 0))
				&& world.isEmptyBlock(pos.offset(0, 0, 0))
				&& world.isEmptyBlock(pos.offset(0, 1, 0))
				&& !world.getBlockState(pos.offset(0, -1, 0)).liquid()
				&& !world.getBlockState(pos.offset(0, 0, 0)).liquid()
				&& !world.getBlockState(pos.offset(0, 1, 0)).liquid();
	}
}
