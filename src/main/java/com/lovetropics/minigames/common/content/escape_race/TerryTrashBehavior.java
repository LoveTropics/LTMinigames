package com.lovetropics.minigames.common.content.escape_race;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.LoveTropics;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public record TerryTrashBehavior (
		String itemSpawnRegion,
		ResourceKey<LootTable> lootTableId,
		Map<TrashType, TrashData> trashData,
		int spawnTime,
		int terryTime,
		List<TrashType> trashPassword,
		String trashLocation,
		String buttonLocation
) implements IGameBehavior {

	public static final MapCodec<TerryTrashBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("item_spawn_region").forGetter(c -> c.itemSpawnRegion),
			ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("loot_table").forGetter(c -> c.lootTableId),
			Codec.unboundedMap(TrashType.CODEC, TrashData.CODEC).fieldOf("trash_data").forGetter(c -> c.trashData),
			Codec.INT.fieldOf("spawn_time").forGetter(c -> c.spawnTime),
			Codec.INT.fieldOf("terry_time").forGetter(c -> c.terryTime),
			TrashType.CODEC.listOf().fieldOf("trash_password").forGetter(c -> c.trashPassword),
			Codec.STRING.fieldOf("trash_location").forGetter(c -> c.trashLocation),
			Codec.STRING.fieldOf("button_location").forGetter(c -> c.buttonLocation)
	).apply(i, TerryTrashBehavior::new));


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
		Map<TrashType, BlockBox> boxRegions = new HashMap<>();
		for (Map.Entry<TrashType, TrashData> trashType : trashData().entrySet()) {
			boxRegions.put(trashType.getKey(), game.mapRegions().getOrThrow(trashType.getValue().processRegion));
		}

		int passwordSize = trashPassword.size();
		Map<BlockPos, TrashType> passwordPositions = new HashMap<>();
		for (int i = 0; i < passwordSize; i++) {
			BlockBox orThrow = game.mapRegions().getOrThrow(trashLocation + "_" + (i + 1));
			if (orThrow.volume() != 1) {
				throw new GameException(Component.literal("Trash location regions must be a single block volume."));
			}
			passwordPositions.put(orThrow.min(), trashPassword.get(i));
		}

		BlockBox buttonLocationBox = game.mapRegions().getOrThrow(buttonLocation);


		GameSidebar sidebar = GlobalGameWidgets.registerTo(game, events).openSidebar(Component.literal("Terry Trash"));

		events.listen(GamePhaseEvents.TICK, () -> onGameTick(game, itemSpawnBox));

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

			if (game.ticks() >= terryTime && buttonLocationBox.contains(pos)) {
				int goodBlocks = 0;
				for (Map.Entry<BlockPos, TrashType> blockPosTrashTypeEntry : passwordPositions.entrySet()) {
					TrashType trashType = blockPosTrashTypeEntry.getValue();
					TrashBlock trashBlockBlockEntry = LoveTropicsBlocks.TRASH.get(trashType).get();
					BlockPos passwordPos = blockPosTrashTypeEntry.getKey();
					AABB aabb = new AABB(passwordPos);
					List<Entity> entities = game.level().getEntities(null, aabb);
					entities.removeIf(entity -> !(entity instanceof ItemFrame));
					ItemFrame itemFrame = (ItemFrame) entities.stream().findFirst().orElse(null);
					if (itemFrame != null) {
						ItemStack frameItem = itemFrame.getItem();
						if (frameItem.getItem() == trashBlockBlockEntry.asItem()) {
							goodBlocks++;
						} else {
							itemFrame.setItem(frameItem);
						}
					}
				}

				game.statistics().global().set(StatisticKey.PASSWORD_CORRECT, goodBlocks);

				sidebar.set(buildSidebar(game));
				return InteractionResult.CONSUME;
			}
			return InteractionResult.SUCCESS;


		}));
	}

	private void onGameTick(IGamePhase game, BlockBox itemSpawnBox) {
		if (game.ticks() % spawnTime == 0) {
			Vec3 center = itemSpawnBox.center();
			BlockPos centerBlock = itemSpawnBox.centerBlock();
			LootTable lootTable = getLootTable(game.server());
			for (ItemStack randomItem : lootTable.getRandomItems(buildLootParams(game.level(), center))) {
				Block.popResource(game.level(), centerBlock, randomItem);
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
		lines.add(Component.literal(game.statistics().global().getInt(StatisticKey.PASSWORD_CORRECT) + " /" + trashPassword.size() + " correct in password"));
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

	private LootTable getLootTable(MinecraftServer server) {
		return server.reloadableRegistries().getLootTable(lootTableId);
	}

}
