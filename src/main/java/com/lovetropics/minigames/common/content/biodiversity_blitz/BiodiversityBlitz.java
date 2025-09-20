package com.lovetropics.minigames.common.content.biodiversity_blitz;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.BbAssignPlotsBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.BbBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.BbClientStateBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.BbCurrencyBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.BbCurrencyWinTrigger;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.BbGivePlantsBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.BbInGameScoreboardBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.BbMerchantBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.BbSendMobsToEnemyItemBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.BbWaveSpawnerBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.EqualizeCurrencyBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.RemoveFromBlockBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.SpawnSurpriseWaveBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant.AgingCropPlantBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant.BarrierPlantBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant.BerriesPlantBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant.EffectAddingPlantBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant.FlamingPlantBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant.FruitDropEntityBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant.GrowCoconutsBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant.GrowPlantBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant.IdleDropItemPlantBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant.LightningPlantBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant.MushroomPlantBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant.PianguasPlantBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant.PlantBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant.PlantBiomeCheckBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant.PlantHealthBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant.PlantItemBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant.ProximityBombPlantBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant.ScareTrapPlantBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant.WateryPlantBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant.drops.DropLootTableBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant.drops.DropPlantItemBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant.placement.PlaceDoublePlantBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant.placement.PlaceFeaturePlantBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant.placement.PlaceSinglePlantBehavior;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.tutorial.BbTutorialAction;
import com.lovetropics.minigames.common.content.biodiversity_blitz.block.BrambleBlock;
import com.lovetropics.minigames.common.content.biodiversity_blitz.block.DirtySandBlock;
import com.lovetropics.minigames.common.content.biodiversity_blitz.client_state.CheckeredPlotsState;
import com.lovetropics.minigames.common.content.biodiversity_blitz.client_state.ClientBbMobSpawnState;
import com.lovetropics.minigames.common.content.biodiversity_blitz.client_state.ClientBbScoreboardState;
import com.lovetropics.minigames.common.content.biodiversity_blitz.client_state.ClientBbSelfState;
import com.lovetropics.minigames.common.content.biodiversity_blitz.client_state.CurrencyItemState;
import com.lovetropics.minigames.common.content.biodiversity_blitz.client_state.CurrencyTargetState;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.BbMobSpawner;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.PlantItemType;
import com.lovetropics.minigames.common.util.registry.GameBehaviorEntry;
import com.lovetropics.minigames.common.util.registry.GameClientTweakEntry;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;
import com.mojang.serialization.Codec;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.generators.RegistrateBlockModelGenerator;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BiodiversityBlitz {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, LoveTropics.ID);

	// Behaviors

	public static final GameBehaviorEntry<BbBehavior> BIODIVERSITY_BLITZ = REGISTRATE.object("biodiversity_blitz")
			.behavior(BbBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<BbAssignPlotsBehavior> ASSIGN_PLOTS = REGISTRATE.object("biodiversity_blitz_assign_plots")
			.behavior(BbAssignPlotsBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<BbMerchantBehavior> MERCHANT = REGISTRATE.object("biodiversity_blitz_merchant")
			.behavior(BbMerchantBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<BbWaveSpawnerBehavior> WAVE_SPAWNER = REGISTRATE.object("biodiversity_blitz_wave_spawner")
			.behavior(BbWaveSpawnerBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<BbSendMobsToEnemyItemBehavior> SEND_MOBS_TO_ENEMY = REGISTRATE.object("biodiversity_blitz_send_mobs_to_enemy")
			.behavior(BbSendMobsToEnemyItemBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<PlantBehavior> PLANT = REGISTRATE.object("biodiversity_blitz_plant")
			.behavior(PlantBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<PlantItemBehavior> PLANT_ITEM = REGISTRATE.object("biodiversity_blitz_plant_item")
			.behavior(PlantItemBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<PlantBiomeCheckBehavior> PLANT_BIOME_CHECK = REGISTRATE.object("plant_biome_check")
			.behavior(PlantBiomeCheckBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<PlaceSinglePlantBehavior> PLACE_SINGLE_PLANT = REGISTRATE.object("place_single_plant")
			.behavior(PlaceSinglePlantBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<PlaceDoublePlantBehavior> PLACE_DOUBLE_PLANT = REGISTRATE.object("place_double_plant")
			.behavior(PlaceDoublePlantBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<PlaceFeaturePlantBehavior> PLACE_FEATURE_PLANT = REGISTRATE.object("place_feature_plant")
			.behavior(PlaceFeaturePlantBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<DropPlantItemBehavior> DROP_PLANT_ITEM = REGISTRATE.object("drop_plant_item")
			.behavior(DropPlantItemBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<DropLootTableBehavior> DROP_LOOT_TABLE = REGISTRATE.object("drop_loot_table")
			.behavior(DropLootTableBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<AgingCropPlantBehavior> AGING_PLANT = REGISTRATE.object("aging_plant")
			.behavior(AgingCropPlantBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<BarrierPlantBehavior> BARRIER_PLANT = REGISTRATE.object("barrier_plant")
			.behavior(BarrierPlantBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<BerriesPlantBehavior> BERRIES_PLANT = REGISTRATE.object("berries_plant")
			.behavior(BerriesPlantBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<EffectAddingPlantBehavior> EFFECT_ADDING_PLANT = REGISTRATE.object("effect_adding_plant")
			.behavior(EffectAddingPlantBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<GrowPlantBehavior> GROW_PLANT = REGISTRATE.object("grow_plant")
			.behavior(GrowPlantBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<PlantHealthBehavior> PLANT_HEALTH = REGISTRATE.object("plant_health")
			.behavior(PlantHealthBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<ProximityBombPlantBehavior> PROXIMITY_BOMB_PLANT = REGISTRATE.object("proximity_bomb_plant")
			.behavior(ProximityBombPlantBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<ScareTrapPlantBehavior> SCARE_TRAP_PLANT = REGISTRATE.object("scare_trap_plant")
			.behavior(ScareTrapPlantBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<FlamingPlantBehavior> FLAMING_PLANT = REGISTRATE.object("flaming_plant")
			.behavior(FlamingPlantBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<WateryPlantBehavior> WATERY_PLANT = REGISTRATE.object("watery_plant")
			.behavior(WateryPlantBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<LightningPlantBehavior> LIGHTNING_PLANT = REGISTRATE.object("lightning_plant")
			.behavior(LightningPlantBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<MushroomPlantBehavior> MUSHROOM_PLANT = REGISTRATE.object("mushroom_plant")
			.behavior(MushroomPlantBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<IdleDropItemPlantBehavior> IDLE_DROP_ITEM = REGISTRATE.object("idle_drop_item")
			.behavior(IdleDropItemPlantBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<PianguasPlantBehavior> PIANGUAS = REGISTRATE.object("pianguas")
			.behavior(PianguasPlantBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<FruitDropEntityBehavior> FRUIT_DROP_ENTITY = REGISTRATE.object("fruit_drop_entity")
			.behavior(FruitDropEntityBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<GrowCoconutsBehavior> GROW_COCONUTS = REGISTRATE.object("grow_coconuts")
			.behavior(GrowCoconutsBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<BbCurrencyBehavior> CURRENCY = REGISTRATE.object("biodiversity_blitz_currency")
			.behavior(BbCurrencyBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<BbClientStateBehavior> SEND_CLIENT_STATE = REGISTRATE.object("biodiversity_blitz_client_state")
			.behavior(BbClientStateBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<RemoveFromBlockBehavior> REMOVE_FROM_BLOCK = REGISTRATE.object("remove_from_block")
            .behavior(RemoveFromBlockBehavior.CODEC)
            .register();

	public static final GameBehaviorEntry<BbCurrencyWinTrigger> CURRENCY_WIN_TRIGGER = REGISTRATE.object("biodiversity_blitz_currency_win_trigger")
			.behavior(BbCurrencyWinTrigger.CODEC)
			.register();

	public static final GameBehaviorEntry<BbGivePlantsBehavior> GIVE_PLANTS = REGISTRATE.object("biodiversity_blitz_give_plants")
			.behavior(BbGivePlantsBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<SpawnSurpriseWaveBehavior> SPAWN_SURPRISE_WAVE = REGISTRATE.object("spawn_surprise_wave")
			.behavior(SpawnSurpriseWaveBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<EqualizeCurrencyBehavior> EQUALIZE_CURRENCY = REGISTRATE.object("equalize_currency")
			.behavior(EqualizeCurrencyBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<BbTutorialAction> BB_TUTORIAL = REGISTRATE.object("bb_tutorial")
			.behavior(BbTutorialAction.CODEC)
			.register();

	public static final GameBehaviorEntry<BbInGameScoreboardBehavior> IG_SCOREBOARD = REGISTRATE.object("bb_in_game_scoreboard")
			.behavior(BbInGameScoreboardBehavior.CODEC)
			.register();

	// Client State

	public static final GameClientTweakEntry<CheckeredPlotsState> CHECKERED_PLOTS_STATE = REGISTRATE.object("checkered_plots")
			.clientState(CheckeredPlotsState.CODEC).streamCodec(CheckeredPlotsState.STREAM_CODEC)
			.register();

	public static final GameClientTweakEntry<CurrencyTargetState> CURRENCY_TARGET = REGISTRATE.object("currency_target")
			.clientState(CurrencyTargetState.CODEC)
			.register();

	public static final GameClientTweakEntry<CurrencyItemState> CURRENCY_ITEM = REGISTRATE.object("currency_item")
			.clientState(CurrencyItemState.CODEC)
			.register();

	public static final GameClientTweakEntry<ClientBbSelfState> SELF_STATE = REGISTRATE.object("biodiversity_blitz_self_state")
			.clientState(ClientBbSelfState.CODEC)
			.register();

	public static final GameClientTweakEntry<ClientBbMobSpawnState> MOB_SPAWN = REGISTRATE.object("mob_spawn")
			.clientState(ClientBbMobSpawnState.CODEC)
			.register();

	public static final GameClientTweakEntry<ClientBbScoreboardState> SCOREBOARD = REGISTRATE.object("scoreboard")
			.clientState(ClientBbScoreboardState.CODEC)
			.register();

	// Items

	public static final ItemEntry<BlockItem> CARROT_SEEDS = REGISTRATE.item("carrot_seeds", p -> new BlockItem(Blocks.CARROTS, p))
			.register();

	public static final ItemEntry<BlockItem> POTATO_SEEDS = REGISTRATE.item("potato_seeds", p -> new BlockItem(Blocks.POTATOES, p))
			.register();

	public static final ItemEntry<BlockItem> SWEET_BERRY_SEEDS = REGISTRATE.item("sweet_berry_seeds", p -> new BlockItem(Blocks.SWEET_BERRY_BUSH, p))
			.register();

	public static final ItemEntry<Item> OSA_POINT = REGISTRATE.item("osa_point", Item::new)
			.lang("Biodiversity Point")
			.register();

	// Blocks

	public static final BlockEntry<DirtySandBlock> DIRTY_SAND = REGISTRATE.block("dirty_sand", DirtySandBlock::new)
			.initialProperties(() -> Blocks.SAND)
			.tag(BlockTags.DIRT, BlockTags.SAND, BlockTags.BAMBOO_PLANTABLE_ON, BlockTags.DRY_VEGETATION_MAY_PLACE_ON)
			.blockstate(() -> (ctx, prov) -> Models.generateCubeLike(ctx, prov, Blocks.SAND))
			.simpleItem()
			.register();

	public static final BlockEntry<BrambleBlock> BRAMBLE = REGISTRATE.block("bramble", BrambleBlock::new)
			.initialProperties(() -> Blocks.SWEET_BERRY_BUSH)
			.addLayer(() -> () -> ChunkSectionLayer.CUTOUT)
			.blockstate(() -> (ctx, prov) ->
					prov.createCrossBlock(ctx.get(), BlockModelGenerators.PlantType.NOT_TINTED)
			)
			.item()
			.model(() -> (ctx, prov) -> prov.generateFlatBlockItem(ctx.get()))
			.build()
			.register();

	// TODO: Move this out of BioBlitz?
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemLore>> SHIFT_LORE = DATA_COMPONENTS.registerComponentType(
			"shift_lore",
			builder -> builder.persistent(ItemLore.CODEC).networkSynchronized(ItemLore.STREAM_CODEC)
	);

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<PlantItemType>> PLANT_COMPONENT = DATA_COMPONENTS.registerComponentType(
			"bb_plant",
			builder -> builder.persistent(PlantItemType.CODEC)
	);

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Map<BbMobSpawner.BbEntityTypes, Integer>>> ENEMIES_TO_SEND = DATA_COMPONENTS.registerComponentType(
			"bb_mobs_to_send_to_enemies",
			builder -> builder.persistent(Codec.unboundedMap(BbMobSpawner.BbEntityTypes.CODEC, ExtraCodecs.NON_NEGATIVE_INT))
	);

	@EventBusSubscriber(modid = LoveTropics.ID)
	public static class Events {
		@SubscribeEvent
		public static void onItemTooltip(ItemTooltipEvent event) {
			ItemStack stack = event.getItemStack();
			List<Component> list = event.getToolTip();

			// Used to pop the advanced tooltip off the stack and add it back at the end
			List<Component> removedComponents = null;

			ItemLore shiftLore = stack.get(SHIFT_LORE);
			if (shiftLore != null) {
				if (event.getFlags().isAdvanced()) {
					removedComponents = new ArrayList<>();
					removedComponents.add(list.removeLast());
					removedComponents.add(list.removeLast());
				}

				if (Screen.hasShiftDown()) {
					list.addAll(shiftLore.styledLines());
				} else {
					list.add(BiodiversityBlitzTexts.SHIFT_FOR_MORE_INFORMATION.copy().withStyle(ChatFormatting.GOLD));
				}
			}

			if (removedComponents != null) {
				list.addAll(removedComponents);
			}
		}
	}

	private static class Models {
		private static void generateCubeLike(DataGenContext<Block, DirtySandBlock> ctx, RegistrateBlockModelGenerator prov, Block textureBlock) {
			prov.generateWithTemplate(ctx.get(), ModelTemplates.CUBE_ALL, TextureMapping.cube(textureBlock));
		}
	}
}
