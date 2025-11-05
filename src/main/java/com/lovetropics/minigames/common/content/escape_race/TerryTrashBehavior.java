package com.lovetropics.minigames.common.content.escape_race;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressChannel;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressionPoint;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.util.GameSidebar;
import com.lovetropics.minigames.common.core.game.util.GlobalGameWidgets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public record TerryTrashBehavior (
		String itemSpawnRegion,
		Map<ProgressionPoint, SpawnTimeData> spawnTimes,
		List<RecylingLocations> recyclingLocations,
		String badTrashLocation
) implements IGameBehavior {

	public static final MapCodec<TerryTrashBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("item_spawn_region").forGetter(c -> c.itemSpawnRegion),
			Codec.unboundedMap(ProgressionPoint.CODEC, SpawnTimeData.CODEC).fieldOf("spawn_times").forGetter(c -> c.spawnTimes),
			RecylingLocations.CODEC.listOf().fieldOf("recycling_locations").forGetter(c -> c.recyclingLocations),
			Codec.STRING.fieldOf("bad_trash_location").forGetter(c -> c.badTrashLocation)
	).apply(i, TerryTrashBehavior::new));

	private record SpawnTimeData(int trashRate, ResourceKey<LootTable> table, ProgressionPoint nextChannel) {
		public static final Codec<SpawnTimeData> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.INT.fieldOf("trash_rate").forGetter(SpawnTimeData::trashRate),
				ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("table").forGetter(SpawnTimeData::table),
				ProgressionPoint.CODEC.fieldOf("next_channel").forGetter(SpawnTimeData::nextChannel)
		).apply(i, SpawnTimeData::new));
	}

	private record RecylingLocations(String processRegion, ItemPredicate itemPredicate) {

		static final Codec<RecylingLocations> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("process_region").forGetter(RecylingLocations::processRegion),
				ItemPredicate.CODEC.fieldOf("item_predicate").forGetter(RecylingLocations::itemPredicate)
		).apply(i, RecylingLocations::new));
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		BlockBox itemSpawnBox = game.mapRegions().getOrThrow(itemSpawnRegion);
		BlockBox badTrashBox = game.mapRegions().getOrThrow(badTrashLocation);
		for (RecylingLocations recyclingLocation : recyclingLocations) {
			game.mapRegions().getOrThrow(recyclingLocation.processRegion);
		}

		GameSidebar sidebar = GlobalGameWidgets.registerTo(game, events).openSidebar(EscapeRaceTexts.TERRY_TRASH);

		events.listen(GamePhaseEvents.TICK, () -> onGameTick(game, sidebar, itemSpawnBox, badTrashBox));

		events.listen(GamePlayerEvents.USE_BLOCK, ((player, world, pos, hand, traceResult) -> {
			ItemStack heldItem = player.getItemInHand(hand);
			for (RecylingLocations recyclingLocation : recyclingLocations) {
				BlockBox box = game.mapRegions().getOrThrow(recyclingLocation.processRegion);
				if (box.contains(pos)) {
					if (recyclingLocation.itemPredicate().test(heldItem)) {
						heldItem.shrink(1);
						game.statistics().global().incrementInt(StatisticKey.RECYCLED_TRASH, 1);
						sidebar.set(buildSidebar(game));
						return InteractionResult.CONSUME;
					} else {
						game.statistics().global().incrementInt(StatisticKey.WRONG_BIN, 1);
						sidebar.set(buildSidebar(game));
						return InteractionResult.FAIL;
					}
				}
			}
			return InteractionResult.SUCCESS_SERVER;
		}));
	}

	private <T extends Entity> void onGameTick(IGamePhase game, GameSidebar sidebar, BlockBox itemSpawnBox, BlockBox badTrashBox) {
		Map< BooleanSupplier, SpawnTimeData> spawnTimeDats = new HashMap<>();
		spawnTimes.forEach((progressionPoint, spawnTimeData) ->
				spawnTimeDats.put(progressionPoint.createPredicate(game, ProgressChannel.MAIN), spawnTimeData));
		for (Map.Entry<BooleanSupplier, SpawnTimeData> entry : spawnTimeDats.entrySet()) {
			SpawnTimeData spawnTimeData = entry.getValue();
			int spawnTime = spawnTimeData.trashRate();
			if (entry.getKey().getAsBoolean() && game.ticks() % spawnTime == 0) {
				if (entry.getValue().nextChannel.createPredicate(game, ProgressChannel.MAIN).getAsBoolean()) {
					continue;
				}
				Vec3 center = itemSpawnBox.center();
				BlockPos centerBlock = itemSpawnBox.centerBlock();
				LootTable lootTable = getLootTable(game.server(), spawnTimeData.table());
				for (ItemStack randomItem : lootTable.getRandomItems(buildLootParams(game.level(), center))) {
					Block.popResource(game.level(), centerBlock, randomItem);
				}
				return;
			}
		}

		for (ItemEntity entitiesOfClass : game.level().getEntitiesOfClass(ItemEntity.class, badTrashBox.asAabb())) {
			game.statistics().global().incrementInt(StatisticKey.MISSED_TRASH, 1);
			entitiesOfClass.discard();
			sidebar.set(buildSidebar(game));
		}

	}

	private Component[] buildSidebar(IGamePhase game) {
		List<Component> lines = new ArrayList<>();
		lines.add(Component.literal("Recycled Trash: " + game.statistics().global().getInt(StatisticKey.RECYCLED_TRASH)));
		lines.add(Component.literal("Wrong Bin: " + game.statistics().global().getInt(StatisticKey.WRONG_BIN)));
		lines.add(Component.literal("Missed Trash: " + game.statistics().global().getInt(StatisticKey.MISSED_TRASH)));
		return lines.toArray(new Component[0]);
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return EscapeRace.TERRY_TRASH;
	}

	private LootParams buildLootParams(ServerLevel level, Vec3 pos) {
		return new LootParams.Builder(level)
				.withParameter(LootContextParams.ORIGIN, pos)
				.withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
				.withParameter(LootContextParams.BLOCK_STATE , level.getBlockState(BlockPos.containing(pos)))
				.create(LootContextParamSets.BLOCK);
	}

	private LootTable getLootTable(MinecraftServer server, ResourceKey<LootTable> lootTableId) {
		return server.reloadableRegistries().getLootTable(lootTableId);
	}

}
