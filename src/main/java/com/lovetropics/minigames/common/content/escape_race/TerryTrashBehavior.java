package com.lovetropics.minigames.common.content.escape_race;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.content.block.LoveTropicsBlocks;
import com.lovetropics.minigames.common.content.block.TrashBlock;
import com.lovetropics.minigames.common.content.block.TrashType;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.GameWinner;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameLogicEvents;
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
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
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
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public record TerryTrashBehavior (
		String itemSpawnRegion,
		Map<ProgressionPoint, SpawnTimeData> spawnTimes,
		Map<TrashType, TrashData> trashData,
		String trashLocation,
		String buttonLocation,
		String badTrashLocation
) implements IGameBehavior {

	public static final MapCodec<TerryTrashBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("item_spawn_region").forGetter(c -> c.itemSpawnRegion),
			Codec.unboundedMap(ProgressionPoint.CODEC, SpawnTimeData.CODEC).fieldOf("spawn_times").forGetter(c -> c.spawnTimes),
			Codec.unboundedMap(TrashType.CODEC, TrashData.CODEC).fieldOf("trash_data").forGetter(c -> c.trashData),
			Codec.STRING.fieldOf("trash_location").forGetter(c -> c.trashLocation),
			Codec.STRING.fieldOf("button_location").forGetter(c -> c.buttonLocation),
			Codec.STRING.fieldOf("bad_trash_location").forGetter(c -> c.badTrashLocation)
	).apply(i, TerryTrashBehavior::new));

	private record SpawnTimeData(int trashRate, ResourceKey<LootTable> table) {
		public static final Codec<SpawnTimeData> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.INT.fieldOf("trash_rate").forGetter(SpawnTimeData::trashRate),
				ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("table").forGetter(SpawnTimeData::table)
		).apply(i, SpawnTimeData::new));
	}

	private record TrashData(String processRegion, int requiredAmount, Optional<ItemPredicate> itemPredicate) {

		static final Codec<TrashData> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("process_region").forGetter(TrashData::processRegion),
				Codec.INT.fieldOf("required_amount").forGetter(TrashData::requiredAmount),
				ItemPredicate.CODEC.optionalFieldOf("item_predicate").forGetter(TrashData::itemPredicate)
		).apply(i, TrashData::new));
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		BlockBox itemSpawnBox = game.mapRegions().getOrThrow(itemSpawnRegion);
		BlockBox badTrashBox = game.mapRegions().getOrThrow(badTrashLocation);
		Map<TrashType, BlockBox> boxRegions = new HashMap<>();
		for (Map.Entry<TrashType, TrashData> trashType : trashData().entrySet()) {
			boxRegions.put(trashType.getKey(), game.mapRegions().getOrThrow(trashType.getValue().processRegion));
		}

		GameSidebar sidebar = GlobalGameWidgets.registerTo(game, events).openSidebar(Component.literal("Terry Trash"));

		events.listen(GamePhaseEvents.TICK, () -> onGameTick(game, itemSpawnBox, badTrashBox));

		events.listen(GamePlayerEvents.ATTACK, (player, target) -> {
			if (target instanceof ItemFrame itemFrame) {
				if (itemFrame.getItem().isEmpty()) {
					return TriState.FALSE;
				}
			}
			return TriState.DEFAULT;
		});

		events.listen(GamePlayerEvents.USE_BLOCK, ((player, world, pos, hand, traceResult) -> {
			ItemStack heldItem = player.getItemInHand(hand);
			for (TrashType trashType : trashData.keySet()) {
				StatisticKey<Integer> statsKey = StatisticKey.TRASH_TYPES.get(trashType);
				BlockBox box = boxRegions.get(trashType);
				if (box.contains(pos) && heldItem.is(LoveTropicsBlocks.TRASH.get(trashType).asItem())) {
					heldItem.shrink(1);
					game.statistics().global().incrementInt(statsKey, 1);
					sidebar.set(buildSidebar(game));
					return InteractionResult.CONSUME;
				}
			}

			return InteractionResult.SUCCESS;
		}));
	}

	private <T extends Entity> void onGameTick(IGamePhase game, BlockBox itemSpawnBox, BlockBox badTrashBox) {
		Map< BooleanSupplier, SpawnTimeData> spawnTimeDats = new HashMap<>();
		spawnTimes.forEach((progressionPoint, spawnTimeData) ->
				spawnTimeDats.put(progressionPoint.createPredicate(game, ProgressChannel.MAIN), spawnTimeData));
		for (Map.Entry<BooleanSupplier, SpawnTimeData> entry : spawnTimeDats.entrySet()) {
			SpawnTimeData spawnTimeData = entry.getValue();
			int spawnTime = spawnTimeData.trashRate();
			if (entry.getKey().getAsBoolean() && game.ticks() % spawnTime == 0) {
				Vec3 center = itemSpawnBox.center();
				BlockPos centerBlock = itemSpawnBox.centerBlock();
				LootTable lootTable = getLootTable(game.server(), spawnTimeData.table());
				for (ItemStack randomItem : lootTable.getRandomItems(buildLootParams(game.level(), center))) {
					Block.popResource(game.level(), centerBlock, randomItem);
				}
				return;
			}
		}

		boolean hasAllTrash = trashData.entrySet().stream().allMatch(trashTypeTrashDataEntry -> {
			TrashType trashType = trashTypeTrashDataEntry.getKey();
			TrashData trashData = trashTypeTrashDataEntry.getValue();
			StatisticKey<Integer> statsKey = StatisticKey.TRASH_TYPES.get(trashType);
			int collected = game.statistics().global().getInt(statsKey);
			return collected >= trashData.requiredAmount();
		});

		if (hasAllTrash) {
			// Todo Fix this
			game.invoker(GameLogicEvents.GAME_OVER).onGameOver(new GameWinner.Nobody());
		}

		for (ItemEntity entitiesOfClass : game.level().getEntitiesOfClass(ItemEntity.class, badTrashBox.asAabb())) {
			game.statistics().global().incrementInt(StatisticKey.MISSED_TRASH, 1);
			entitiesOfClass.discard();
		}

	}

	private Component[] buildSidebar(IGamePhase game) {
		List<Component> lines = new ArrayList<>();
		for (Map.Entry<TrashType, TrashData> trashTypeTrashDataEntry : trashData.entrySet()) {
			TrashType trashType = trashTypeTrashDataEntry.getKey();
			TrashData trashData = trashTypeTrashDataEntry.getValue();
			StatisticKey<Integer> statsKey = StatisticKey.TRASH_TYPES.get(trashType);
			int collected = game.statistics().global().getInt(statsKey);
			Component line = Component.empty()
					.append(Component.literal(trashType.getId() + " "))
					.append(Component.literal(collected + ""))
					.append(Component.literal(" /"))
					.append(Component.literal(trashData.requiredAmount() + ""));
			lines.add(line);
		}
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
