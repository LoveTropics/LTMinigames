package org.lovetropics.games.common.core.game.behavior.instances;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.TriState;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerConfig;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableInt;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameWorldEvents;
import org.lovetropics.games.common.core.game.region.Region;
import org.lovetropics.games.common.core.game.region.RegionQuery;
import org.lovetropics.games.common.util.Codecs;
import org.lovetropics.games.mixin.TrialSpawnerAccess;

import java.util.List;
import java.util.Map;

public record OrderedTrialSpawnerBehavior(
		Map<RegionQuery, Holder<TrialSpawnerConfig>> spawnerRegions,
		List<Holder<LootTable>> lootTables,
		int targetCooldownLength,
		int requiredPlayerRange
) implements IGameBehavior {
	public static final MapCodec<OrderedTrialSpawnerBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.unboundedMap(RegionQuery.CODEC, TrialSpawnerConfig.CODEC).fieldOf("spawner_regions").forGetter(OrderedTrialSpawnerBehavior::spawnerRegions),
			Codecs.LOOT_TABLE.listOf().fieldOf("loot_tables").forGetter(OrderedTrialSpawnerBehavior::lootTables),
			ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("target_cooldown_length", 36000).forGetter(OrderedTrialSpawnerBehavior::targetCooldownLength),
			Codec.intRange(1, 128).optionalFieldOf("required_player_range", 14).forGetter(OrderedTrialSpawnerBehavior::requiredPlayerRange)
	).apply(i, OrderedTrialSpawnerBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		List<Spawner> spawners = spawnerRegions.entrySet().stream()
				.map(entry -> {
					Region region = entry.getKey().one(game);
					return new Spawner(region.level(), region.centerBlock(), entry.getValue());
				})
				.collect(Util.toMutableList());

		events.listen(GameWorldEvents.CHUNK_LOAD, (level, chunk) -> {
			for (Spawner spawner : spawners) {
				if (!spawner.isInChunk(level, chunk.getPos())) {
					continue;
				}
				chunk.setBlockState(spawner.pos(), Blocks.TRIAL_SPAWNER.defaultBlockState());
				if (chunk.getBlockEntity(spawner.pos()) instanceof TrialSpawnerBlockEntity blockEntity) {
					TrialSpawnerAccess trialSpawner = (TrialSpawnerAccess) (Object) blockEntity.getTrialSpawner();
					trialSpawner.setConfig(new TrialSpawner.FullConfig(
							spawner.config,
							spawner.config,
							targetCooldownLength,
							requiredPlayerRange
					));
				}
			}
		});

		MutableInt nextLootTableIndex = new MutableInt(0);

		events.listen(GameWorldEvents.TRIAL_SPAWNER_EJECT_LOOT, (level, pos, trialSpawner) -> {
			if (spawners.stream().noneMatch(spawner -> spawner.level == level && spawner.pos.equals(pos))) {
				return false;
			}
			int index = nextLootTableIndex.getAndIncrement();
			if (index >= lootTables.size()) {
				return false;
			}
			Holder<LootTable> lootTable = lootTables.get(index);
			LootParams params = new LootParams.Builder(level).create(LootContextParamSets.EMPTY);
			for (ItemStack item : lootTable.value().getRandomItems(params)) {
				DefaultDispenseItemBehavior.spawnItem(level, item, 2, Direction.UP, Vec3.atBottomCenterOf(pos).relative(Direction.UP, 1.2));
			}
			level.levelEvent(LevelEvent.ANIMATION_TRIAL_SPAWNER_EJECT_ITEM, pos, 0);
			return true;
		});

		events.listen(GameWorldEvents.SPAWN_PLACEMENT_CHECK, (level, pos, reason, entityType) -> {
			if (reason != EntitySpawnReason.TRIAL_SPAWNER) {
				return TriState.DEFAULT;
			}
			BlockPos belowPos = pos.below();
			return level.getBlockState(belowPos).isCollisionShapeFullBlock(level, belowPos) ? TriState.TRUE : TriState.DEFAULT;
		});
	}

	private record Spawner(ServerLevel level, BlockPos pos, Holder<TrialSpawnerConfig> config) {
		public boolean isInChunk(ServerLevel level, ChunkPos chunkPos) {
			return level == this.level && chunkPos.equals(ChunkPos.containing(pos));
		}
	}
}
