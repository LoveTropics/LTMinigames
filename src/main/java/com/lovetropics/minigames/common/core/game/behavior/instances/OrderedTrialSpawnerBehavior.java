package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameWorldEvents;
import com.lovetropics.minigames.mixin.TrialSpawnerAccess;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerConfig;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public record OrderedTrialSpawnerBehavior(
		Map<String, Holder<TrialSpawnerConfig>> spawnerRegions,
		List<Holder<LootTable>> lootTables,
		int targetCooldownLength,
		int requiredPlayerRange
) implements IGameBehavior {
	private static final Codec<Holder<LootTable>> LOOT_TABLE_CODEC = Codec.either(MoreCodecs.ITEM_STACK, LootTable.CODEC).xmap(
			either -> either.map(
					itemStack -> {
						LootPoolSingletonContainer.Builder<?> item = LootItem.lootTableItem(itemStack.getItem());
						for (Map.Entry<DataComponentType<?>, Optional<?>> entry : itemStack.getComponentsPatch().entrySet()) {
							item = applyComponent(itemStack, item, entry.getKey());
						}
						return Holder.direct(LootTable.lootTable()
								.withPool(LootPool.lootPool()
										.setRolls(ConstantValue.exactly(1))
										.add(item.apply(SetItemCountFunction.setCount(ConstantValue.exactly(itemStack.getCount()))))
								)
								.build());
					},
					Function.identity()
			),
			Either::right
	);

	private static <T> LootPoolSingletonContainer.Builder<?> applyComponent(ItemStack itemStack, LootPoolSingletonContainer.Builder<?> item, DataComponentType<T> component) {
		T value = itemStack.get(component);
		if (value == null) {
			return item;
		}
		return item.apply(SetComponentsFunction.setComponent(component, value));
	}

	public static final MapCodec<OrderedTrialSpawnerBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.unboundedMap(Codec.STRING, TrialSpawnerConfig.CODEC).fieldOf("spawner_regions").forGetter(OrderedTrialSpawnerBehavior::spawnerRegions),
			LOOT_TABLE_CODEC.listOf().fieldOf("loot_tables").forGetter(OrderedTrialSpawnerBehavior::lootTables),
			ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("target_cooldown_length", 36000).forGetter(OrderedTrialSpawnerBehavior::targetCooldownLength),
			Codec.intRange(1, 128).optionalFieldOf("required_player_range", 14).forGetter(OrderedTrialSpawnerBehavior::requiredPlayerRange)
	).apply(i, OrderedTrialSpawnerBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		List<Spawner> spawners = spawnerRegions.entrySet().stream()
				.map(entry -> {
					BlockBox box = game.mapRegions().getOrThrow(entry.getKey());
					return new Spawner(box.centerBlock(), entry.getValue());
				})
				.collect(Util.toMutableList());

		events.listen(GameWorldEvents.CHUNK_LOAD, chunk -> {
			for (Spawner spawner : spawners) {
				if (!spawner.isInChunk(chunk.getPos())) {
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

		events.listen(GameWorldEvents.TRIAL_SPAWNER_EJECT_LOOT, (pos, trialSpawner) -> {
			if (spawners.stream().noneMatch(spawner -> spawner.pos.equals(pos))) {
				return false;
			}
			int index = nextLootTableIndex.getAndIncrement();
			if (index >= lootTables.size()) {
				return false;
			}
			Holder<LootTable> lootTable = lootTables.get(index);
			LootParams params = new LootParams.Builder(game.level()).create(LootContextParamSets.EMPTY);
			for (ItemStack item : lootTable.value().getRandomItems(params)) {
				DefaultDispenseItemBehavior.spawnItem(game.level(), item, 2, Direction.UP, Vec3.atBottomCenterOf(pos).relative(Direction.UP, 1.2));
			}
			game.level().levelEvent(LevelEvent.ANIMATION_TRIAL_SPAWNER_EJECT_ITEM, pos, 0);
			return true;
		});

		events.listen(GameWorldEvents.SPAWN_PLACEMENT_CHECK, (pos, reason, entityType) -> {
			if (reason != EntitySpawnReason.TRIAL_SPAWNER) {
				return TriState.DEFAULT;
			}
			BlockPos belowPos = pos.below();
			return game.level().getBlockState(belowPos).isCollisionShapeFullBlock(game.level(), belowPos) ? TriState.TRUE : TriState.DEFAULT;
		});
	}

	private record Spawner(BlockPos pos, Holder<TrialSpawnerConfig> config) {
		public boolean isInChunk(ChunkPos chunkPos) {
			return chunkPos.equals(new ChunkPos(pos));
		}
	}
}
