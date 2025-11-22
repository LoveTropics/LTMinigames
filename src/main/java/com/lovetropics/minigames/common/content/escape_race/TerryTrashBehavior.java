package com.lovetropics.minigames.common.content.escape_race;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.SoundRegistry;
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
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.lovetropics.minigames.common.core.game.util.GameSidebar;
import com.lovetropics.minigames.common.core.game.util.GameWidgets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.CommonColors;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
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

public final class TerryTrashBehavior implements IGameBehavior {

	public static final MapCodec<TerryTrashBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("item_spawn_region").forGetter(c -> c.itemSpawnRegion),
			Codec.unboundedMap(ProgressionPoint.CODEC, SpawnTimeData.CODEC).fieldOf("spawn_times").forGetter(c -> c.spawnTimes),
			RecylingLocations.CODEC.listOf().fieldOf("recycling_locations").forGetter(c -> c.recyclingLocations),
			Codec.STRING.fieldOf("bad_trash_location").forGetter(c -> c.badTrashLocation),
			Codec.STRING.fieldOf("check_lever").forGetter(c -> c.checkLever),
			CodeCheck.CODEC.listOf().fieldOf("code_checks").forGetter(c -> c.codeChecks)
	).apply(i, TerryTrashBehavior::new));

	private final String itemSpawnRegion;
	private final Map<ProgressionPoint, SpawnTimeData> spawnTimes;
	private final List<RecylingLocations> recyclingLocations;
	private final String badTrashLocation;
	private final String checkLever;
	private final List<CodeCheck> codeChecks;

	public TerryTrashBehavior(
			String itemSpawnRegion,
			Map<ProgressionPoint, SpawnTimeData> spawnTimes,
			List<RecylingLocations> recyclingLocations,
			String badTrashLocation,
			String checkLever,
			List<CodeCheck> codeChecks
	) {
		this.itemSpawnRegion = itemSpawnRegion;
		this.spawnTimes = spawnTimes;
		this.recyclingLocations = recyclingLocations;
		this.badTrashLocation = badTrashLocation;
		this.checkLever = checkLever;
		this.codeChecks = codeChecks;
	}

	public record SpawnTimeData(int trashRate, ResourceKey<LootTable> table, ProgressionPoint nextChannel) {
		public static final Codec<SpawnTimeData> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.INT.fieldOf("trash_rate").forGetter(SpawnTimeData::trashRate),
				ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("table").forGetter(SpawnTimeData::table),
				ProgressionPoint.CODEC.fieldOf("next_channel").forGetter(SpawnTimeData::nextChannel)
		).apply(i, SpawnTimeData::new));
	}

	public record CodeCheck(String blockRegion, String lightRegion, BlockPredicate blockPredicate, BlockStateProvider goodCode, BlockStateProvider badCode, BlockStateProvider clearState) {
		public static final Codec<CodeCheck> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("block_region").forGetter(CodeCheck::blockRegion),
				Codec.STRING.fieldOf("light_region").forGetter(CodeCheck::lightRegion),
				BlockPredicate.CODEC.fieldOf("block_predicate").forGetter(CodeCheck::blockPredicate),
				MoreCodecs.BLOCK_STATE_PROVIDER.fieldOf("good_code").forGetter(CodeCheck::goodCode),
				MoreCodecs.BLOCK_STATE_PROVIDER.fieldOf("bad_code").forGetter(CodeCheck::badCode),
				MoreCodecs.BLOCK_STATE_PROVIDER.fieldOf("clear_state").forGetter(CodeCheck::clearState)
		).apply(i, CodeCheck::new));
	}

	public record RecylingLocations(String processRegion, ItemPredicate itemPredicate) {

		static final Codec<RecylingLocations> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("process_region").forGetter(RecylingLocations::processRegion),
				ItemPredicate.CODEC.fieldOf("item_predicate").forGetter(RecylingLocations::itemPredicate)
		).apply(i, RecylingLocations::new));
	}

	private boolean codeGood = false;

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		BlockBox itemSpawnBox = game.mapRegions().getOrThrow(itemSpawnRegion);
		BlockBox badTrashBox = game.mapRegions().getOrThrow(badTrashLocation);
		for (RecylingLocations recyclingLocation : recyclingLocations) {
			game.mapRegions().getOrThrow(recyclingLocation.processRegion);
		}

		GameSidebar sidebar = GameWidgets.getOrRegister(game, events).openGlobalSidebar(EscapeRaceTexts.TERRY_TRASH);

		events.listen(GamePhaseEvents.TICK, () -> onGameTick(game, sidebar, itemSpawnBox, badTrashBox));


		TeamState teams = game.instanceState().getOrThrow(TeamState.KEY);

		events.listen(GamePlayerEvents.USE_BLOCK, ((player, world, pos, hand, traceResult) -> {
			ItemStack heldItem = player.getItemInHand(hand);
			if (game.mapRegions().getOrThrow(checkLever).contains(pos)) {
				if (codeGood) {
					return InteractionResult.FAIL;
				}
				boolean allMatch = true;
				for (CodeCheck codeCheck : codeChecks) {
					BlockPos blockBox = game.mapRegions().getOrThrow(codeCheck.blockRegion).min();
					BlockPos lightBox = game.mapRegions().getOrThrow(codeCheck.lightRegion).min();
					if (codeCheck.blockPredicate.matches(world, blockBox)) {
						BlockState goodBlock = codeCheck.goodCode.getState(world.random, blockBox);
						world.setBlockAndUpdate(lightBox, goodBlock);
					} else {
						BlockState badBlock = codeCheck.badCode.getState(world.random, blockBox);
						world.setBlockAndUpdate(lightBox, badBlock);
						allMatch = false;
					}
					world.setBlockAndUpdate(blockBox, codeCheck.clearState.getState(world.random, blockBox));
				}
				if (allMatch) {
					codeGood = allMatch;
					player.playNotifySound(SoundRegistry.CORRECT.value(), SoundSource.BLOCKS, 1.0f, 1.0f);
					GameTeamKey teamForPlayer = teams.getTeamForPlayer(player);
					if (teamForPlayer != null) {
						game.statistics().forTeam(teamForPlayer).incrementInt(StatisticKey.VACATION_DAYS, 2);
					}
				} else {
					player.playNotifySound(SoundRegistry.INCORRECT.value(), SoundSource.BLOCKS, 1.0f, 1.0f);
					player.sendSystemMessage(Component.literal("Invalid Code!").withColor(CommonColors.RED));
				}
				return InteractionResult.SUCCESS;
			}
			if (heldItem.isEmpty()) {
				return InteractionResult.PASS;
			}
			for (RecylingLocations recyclingLocation : recyclingLocations) {
				BlockBox box = game.mapRegions().getOrThrow(recyclingLocation.processRegion);
				if (box.contains(pos)) {
					if (recyclingLocation.itemPredicate().test(heldItem)) {
						heldItem.shrink(1);
						game.statistics().global().incrementInt(StatisticKey.RECYCLED_TRASH, 1);
						player.playNotifySound(SoundRegistry.CORRECT.value(), SoundSource.BLOCKS, 1.0f, 1.0f);
						sidebar.set(buildSidebar(game));
						return InteractionResult.SUCCESS_SERVER;
					} else {
						heldItem.shrink(1);
						game.statistics().global().incrementInt(StatisticKey.WRONG_BIN, 1);
						player.playNotifySound(SoundRegistry.INCORRECT.value(), SoundSource.BLOCKS, 1.0f, 1.0f);
						sidebar.set(buildSidebar(game));
						return InteractionResult.SUCCESS_SERVER;
					}
				}
			}
			return InteractionResult.PASS;
		}));
	}

	private void onGameTick(IGamePhase game, GameSidebar sidebar, BlockBox itemSpawnBox, BlockBox badTrashBox) {
		Map<BooleanSupplier, SpawnTimeData> spawnTimeDats = new HashMap<>();
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
		lines.add(Component.literal("Recycled Trash: ").append(Component.literal(String.valueOf(game.statistics().global().getInt(StatisticKey.RECYCLED_TRASH))).withStyle(ChatFormatting.AQUA)));
		lines.add(Component.literal("Wrong Bin: ").append(Component.literal(String.valueOf(game.statistics().global().getInt(StatisticKey.WRONG_BIN))).withStyle(ChatFormatting.RED)));
		lines.add(Component.literal("Missed Trash: ").append(Component.literal(String.valueOf(game.statistics().global().getInt(StatisticKey.MISSED_TRASH))).withStyle(ChatFormatting.RED)));
		lines.add(Component.literal("Code Status: " + (codeGood ? "Good" : "Bad")));
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
				.withParameter(LootContextParams.BLOCK_STATE, level.getBlockState(BlockPos.containing(pos)))
				.create(LootContextParamSets.BLOCK);
	}

	private LootTable getLootTable(MinecraftServer server, ResourceKey<LootTable> lootTableId) {
		return server.reloadableRegistries().getLootTable(lootTableId);
	}
}
