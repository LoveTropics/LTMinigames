package org.lovetropics.games.common.content.biodiversity_blitz;

import com.mojang.serialization.Codec;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.generators.RegistrateBlockModelGenerator;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
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
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.BbAssignPlotsBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.BbBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.BbClientStateBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.BbCurrencyBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.BbCurrencyWinTrigger;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.BbGivePlantsBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.BbInGameScoreboardBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.BbMerchantBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.BbSendMobsToEnemyItemBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.BbWaveSpawnerBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.EqualizeCurrencyBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.RemoveFromBlockBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.SpawnSurpriseWaveBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.plant.AgingCropPlantBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.plant.BarrierPlantBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.plant.BerriesPlantBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.plant.EffectAddingPlantBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.plant.FlamingPlantBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.plant.FruitDropEntityBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.plant.GrowCoconutsBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.plant.GrowPlantBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.plant.IdleDropItemPlantBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.plant.LightningPlantBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.plant.MushroomPlantBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.plant.PianguasPlantBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.plant.PlantBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.plant.PlantBiomeCheckBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.plant.PlantHealthBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.plant.PlantItemBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.plant.ProximityBombPlantBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.plant.ScareTrapPlantBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.plant.WateryPlantBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.plant.drops.DropLootTableBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.plant.drops.DropPlantItemBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.plant.placement.PlaceDoublePlantBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.plant.placement.PlaceFeaturePlantBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.plant.placement.PlaceSinglePlantBehavior;
import org.lovetropics.games.common.content.biodiversity_blitz.behavior.tutorial.BbTutorialAction;
import org.lovetropics.games.common.content.biodiversity_blitz.block.BrambleBlock;
import org.lovetropics.games.common.content.biodiversity_blitz.block.DirtySandBlock;
import org.lovetropics.games.common.content.biodiversity_blitz.client_state.CheckeredPlotsState;
import org.lovetropics.games.common.content.biodiversity_blitz.client_state.ClientBbMobSpawnState;
import org.lovetropics.games.common.content.biodiversity_blitz.client_state.ClientBbScoreboardState;
import org.lovetropics.games.common.content.biodiversity_blitz.client_state.ClientBbSelfState;
import org.lovetropics.games.common.content.biodiversity_blitz.client_state.CurrencyItemState;
import org.lovetropics.games.common.content.biodiversity_blitz.client_state.CurrencyTargetState;
import org.lovetropics.games.common.content.biodiversity_blitz.entity.BbMobSpawner;
import org.lovetropics.games.common.content.biodiversity_blitz.plot.plant.PlantItemType;
import org.lovetropics.games.common.util.registry.GameClientTweakEntry;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BiodiversityBlitz {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, LoveTropics.ID);

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
			.tag(BlockTags.DIRT, BlockTags.SAND, BlockTags.SUPPORTS_BAMBOO, BlockTags.SUPPORTS_DRY_VEGETATION)
			.blockstate(() -> (ctx, prov) -> Models.generateCubeLike(ctx, prov, Blocks.SAND))
			.simpleItem()
			.register();

	public static final BlockEntry<BrambleBlock> BRAMBLE = REGISTRATE.block("bramble", BrambleBlock::new)
			.initialProperties(() -> Blocks.SWEET_BERRY_BUSH)
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

	static {
		REGISTRATE.registerBehavior("biodiversity_blitz", BbBehavior.CODEC);
		REGISTRATE.registerBehavior("biodiversity_blitz_assign_plots", BbAssignPlotsBehavior.CODEC);
		REGISTRATE.registerBehavior("biodiversity_blitz_merchant", BbMerchantBehavior.CODEC);
		REGISTRATE.registerBehavior("biodiversity_blitz_wave_spawner", BbWaveSpawnerBehavior.CODEC);
		REGISTRATE.registerBehavior("biodiversity_blitz_send_mobs_to_enemy", BbSendMobsToEnemyItemBehavior.CODEC);
		REGISTRATE.registerBehavior("biodiversity_blitz_plant", PlantBehavior.CODEC);
		REGISTRATE.registerBehavior("biodiversity_blitz_plant_item", PlantItemBehavior.CODEC);
		REGISTRATE.registerBehavior("plant_biome_check", PlantBiomeCheckBehavior.CODEC);
		REGISTRATE.registerBehavior("place_single_plant", PlaceSinglePlantBehavior.CODEC);
		REGISTRATE.registerBehavior("place_double_plant", PlaceDoublePlantBehavior.CODEC);
		REGISTRATE.registerBehavior("place_feature_plant", PlaceFeaturePlantBehavior.CODEC);
		REGISTRATE.registerBehavior("drop_plant_item", DropPlantItemBehavior.CODEC);
		REGISTRATE.registerBehavior("drop_loot_table", DropLootTableBehavior.CODEC);
		REGISTRATE.registerBehavior("aging_plant", AgingCropPlantBehavior.CODEC);
		REGISTRATE.registerBehavior("barrier_plant", BarrierPlantBehavior.CODEC);
		REGISTRATE.registerBehavior("berries_plant", BerriesPlantBehavior.CODEC);
		REGISTRATE.registerBehavior("effect_adding_plant", EffectAddingPlantBehavior.CODEC);
		REGISTRATE.registerBehavior("grow_plant", GrowPlantBehavior.CODEC);
		REGISTRATE.registerBehavior("plant_health", PlantHealthBehavior.CODEC);
		REGISTRATE.registerBehavior("proximity_bomb_plant", ProximityBombPlantBehavior.CODEC);
		REGISTRATE.registerBehavior("scare_trap_plant", ScareTrapPlantBehavior.CODEC);
		REGISTRATE.registerBehavior("flaming_plant", FlamingPlantBehavior.CODEC);
		REGISTRATE.registerBehavior("watery_plant", WateryPlantBehavior.CODEC);
		REGISTRATE.registerBehavior("lightning_plant", LightningPlantBehavior.CODEC);
		REGISTRATE.registerBehavior("mushroom_plant", MushroomPlantBehavior.CODEC);
		REGISTRATE.registerBehavior("idle_drop_item", IdleDropItemPlantBehavior.CODEC);
		REGISTRATE.registerBehavior("pianguas", PianguasPlantBehavior.CODEC);
		REGISTRATE.registerBehavior("fruit_drop_entity", FruitDropEntityBehavior.CODEC);
		REGISTRATE.registerBehavior("grow_coconuts", GrowCoconutsBehavior.CODEC);
		REGISTRATE.registerBehavior("biodiversity_blitz_currency", BbCurrencyBehavior.CODEC);
		REGISTRATE.registerBehavior("biodiversity_blitz_client_state", BbClientStateBehavior.CODEC);
		REGISTRATE.registerBehavior("remove_from_block", RemoveFromBlockBehavior.CODEC);
		REGISTRATE.registerBehavior("biodiversity_blitz_currency_win_trigger", BbCurrencyWinTrigger.CODEC);
		REGISTRATE.registerBehavior("biodiversity_blitz_give_plants", BbGivePlantsBehavior.CODEC);
		REGISTRATE.registerBehavior("spawn_surprise_wave", SpawnSurpriseWaveBehavior.CODEC);
		REGISTRATE.registerBehavior("equalize_currency", EqualizeCurrencyBehavior.CODEC);
		REGISTRATE.registerBehavior("bb_tutorial", BbTutorialAction.CODEC);
		REGISTRATE.registerBehavior("bb_in_game_scoreboard", BbInGameScoreboardBehavior.CODEC);
	}

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

				if (Minecraft.getInstance().hasShiftDown()) {
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
