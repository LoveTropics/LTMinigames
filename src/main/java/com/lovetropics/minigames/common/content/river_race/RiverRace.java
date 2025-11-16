package com.lovetropics.minigames.common.content.river_race;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.river_race.behaviour.CollectablesBehaviour;
import com.lovetropics.minigames.common.content.river_race.behaviour.KillAboveVoidBehavior;
import com.lovetropics.minigames.common.content.river_race.behaviour.ModifyMaxSpawnsAction;
import com.lovetropics.minigames.common.content.river_race.behaviour.ProgressBehaviour;
import com.lovetropics.minigames.common.content.river_race.behaviour.RewardsFromMicrogameBehavior;
import com.lovetropics.minigames.common.content.river_race.behaviour.RiverRaceMerchantBehavior;
import com.lovetropics.minigames.common.content.river_race.behaviour.RiverRaceSetupBehavior;
import com.lovetropics.minigames.common.content.river_race.behaviour.RiverRaceSpawnsBehavior;
import com.lovetropics.minigames.common.content.river_race.behaviour.RiverRaceZoneBehavior;
import com.lovetropics.minigames.common.content.river_race.behaviour.StartMicrogamesAction;
import com.lovetropics.minigames.common.content.river_race.behaviour.TriviaBehaviour;
import com.lovetropics.minigames.common.content.river_race.behaviour.UnlockZoneAction;
import com.lovetropics.minigames.common.content.river_race.behaviour.VictoryPointsBehavior;
import com.lovetropics.minigames.common.content.river_race.block.TriviaBlock;
import com.lovetropics.minigames.common.content.river_race.block.TriviaBlockEntity;
import com.lovetropics.minigames.common.content.river_race.block.TriviaChestBlock;
import com.lovetropics.minigames.common.content.river_race.block.TriviaChestBlockEntity;
import com.lovetropics.minigames.common.content.river_race.client_state.RiverRaceClientBarState;
import com.lovetropics.minigames.common.util.registry.GameBehaviorEntry;
import com.lovetropics.minigames.common.util.registry.GameClientTweakEntry;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.generators.RegistrateBlockModelGenerator;
import com.tterrag.registrate.providers.generators.RegistrateItemModelGenerator;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.renderer.special.ChestSpecialRenderer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static net.minecraft.client.data.models.BlockModelGenerators.createBooleanModelDispatch;
import static net.minecraft.client.data.models.BlockModelGenerators.plainVariant;

public class RiverRace {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, LoveTropics.ID);

	public static final GameBehaviorEntry<RiverRaceSetupBehavior> SETUP_BEHAVIOR = REGISTRATE.object("river_race/setup").behavior(RiverRaceSetupBehavior.CODEC).register();
	public static final GameBehaviorEntry<RiverRaceZoneBehavior> ZONE_BEHAVIOR = REGISTRATE.object("river_race/zone").behavior(RiverRaceZoneBehavior.CODEC).register();
	public static final GameBehaviorEntry<RiverRaceSpawnsBehavior> SPAWNS_BEHAVIOUR = REGISTRATE.object("river_race/spawns").behavior(RiverRaceSpawnsBehavior.CODEC).register();
	public static final GameBehaviorEntry<ModifyMaxSpawnsAction> MODIFY_MAX_SPAWNS_ACTION = REGISTRATE.object("river_race/modify_max_spawns").behavior(ModifyMaxSpawnsAction.CODEC).register();
	public static final GameBehaviorEntry<TriviaBehaviour> TRIVIA_BEHAVIOUR = REGISTRATE.object("trivia").behavior(TriviaBehaviour.CODEC).register();
	public static final GameBehaviorEntry<StartMicrogamesAction> START_MICROGAMES_ACTION = REGISTRATE.object("start_microgames").behavior(StartMicrogamesAction.CODEC).register();
	public static final GameBehaviorEntry<VictoryPointsBehavior> VICTORY_POINTS_BEHAVIOR = REGISTRATE.object("victory_points").behavior(VictoryPointsBehavior.CODEC).register();
	public static final GameBehaviorEntry<RiverRaceMerchantBehavior> RIVER_RACE_MERCHANT_BEHAVIOR = REGISTRATE.object("river_race_merchant").behavior(RiverRaceMerchantBehavior.CODEC).register();
	public static final GameBehaviorEntry<ProgressBehaviour> RIVER_RACE_PROGRESS_BEHAVIOUR = REGISTRATE.object("river_race_progress").behavior(ProgressBehaviour.CODEC).register();
	public static final GameBehaviorEntry<CollectablesBehaviour> COLLECTABLES_BEHAVIOUR = REGISTRATE.object("river_race_collectables").behavior(CollectablesBehaviour.CODEC).register();
	public static final GameBehaviorEntry<UnlockZoneAction> UNLOCK_ZONE_ACTION = REGISTRATE.object("unlock_zone").behavior(UnlockZoneAction.CODEC).register();
	public static final GameBehaviorEntry<KillAboveVoidBehavior> KILL_ABOVE_VOID_BEHAVIOR = REGISTRATE.object("kill_above_void").behavior(KillAboveVoidBehavior.CODEC).register();
	public static final GameBehaviorEntry<RewardsFromMicrogameBehavior> REWARDS_FROM_MICROGAME_BEHAVIOR = REGISTRATE.object("rewards_from_microgame").behavior(RewardsFromMicrogameBehavior.CODEC).register();

	public static final GameClientTweakEntry<RiverRaceClientBarState> BAR_STATE = REGISTRATE.object("river_race_bar")
			.clientState(RiverRaceClientBarState.CODEC).streamCodec(RiverRaceClientBarState.STREAM_CODEC)
			.register();

	public static final BlockEntry<TriviaBlock.GateTriviaBlock> TRIVIA_GATE = REGISTRATE
			.block("trivia_gate", TriviaBlock.GateTriviaBlock::new)
			.initialProperties(() -> Blocks.BEDROCK)
			.properties(BlockBehaviour.Properties::noLootTable)
			.blockstate(() -> (ctx, prov) -> Models.generateTriviaBlock(ctx, prov, true))
			.simpleItem()
			.register();

	public static final BlockEntry<TriviaBlock.CollectableTriviaBlock> TRIVIA_COLLECTABLE = REGISTRATE
			.block("trivia_collectable", TriviaBlock.CollectableTriviaBlock::new)
			.initialProperties(() -> Blocks.BEDROCK)
			.properties(BlockBehaviour.Properties::noLootTable)
			.blockstate(() -> (ctx, prov) -> Models.generateTriviaBlock(ctx, prov, false))
			.simpleItem()
			.register();
	public static final BlockEntry<TriviaBlock.VictoryTriviaBlock> TRIVIA_VICTORY = REGISTRATE
			.block("trivia_victory", TriviaBlock.VictoryTriviaBlock::new)
			.initialProperties(() -> Blocks.BEDROCK)
			.properties(BlockBehaviour.Properties::noLootTable)
			.blockstate(() -> (ctx, prov) -> Models.generateTriviaBlock(ctx, prov, true))
			.simpleItem()
			.register();

	public static final BlockEntry<TriviaChestBlock> TRIVIA_CHEST = REGISTRATE
			.block("trivia_chest", TriviaChestBlock::new)
			.initialProperties(() -> Blocks.BEDROCK)
			.properties(BlockBehaviour.Properties::noLootTable)
			.blockstate(() -> (ctx, prov) -> prov.createParticleOnlyBlock(ctx.get(), TRIVIA_VICTORY.get()))
			.blockEntity(TriviaChestBlockEntity::new)
			.build()
			.item()
			.model(() -> (ctx, prov) ->
					Models.generateChestItem(ctx, prov, LoveTropics.location("trivia"), TextureMapping.getBlockTexture(TRIVIA_VICTORY.get()))
			)
			.build()
			.addMiscData(ProviderType.LANG, prov -> prov.add(LoveTropics.ID + ".container.triviaChest", "Trivia Chest"))
			.register();

	public static final BlockEntityEntry<TriviaBlockEntity> TRIVIA_BLOCK_ENTITY =
			REGISTRATE.blockEntity("trivia_block_entity", TriviaBlockEntity::new)
					.validBlocks(TRIVIA_GATE, TRIVIA_COLLECTABLE, TRIVIA_VICTORY).register();
	public static final BlockEntityEntry<TriviaChestBlockEntity> TRIVIA_CHEST_BLOCK_ENTITY = BlockEntityEntry.cast(REGISTRATE.get("trivia_chest", Registries.BLOCK_ENTITY_TYPE));

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> COLLECTABLE_MARKER = DATA_COMPONENTS.registerComponentType(
			"river_race_collectable_marker",
			builder -> builder.persistent(Unit.CODEC)
	);

	public static void init() {
	}

	private static class Models {
		private static final TextureSlot GLOW_SLOT = TextureSlot.create("glow");

		private static final ModelTemplate CUBE_GLOW_TEMPLATE = ModelTemplates.create(LoveTropics.location("cube_glow").toString(), TextureSlot.ALL, GLOW_SLOT);

		private static void generateTriviaBlock(DataGenContext<Block, ?> ctx, RegistrateBlockModelGenerator prov, boolean useInactiveTexture) {
			MultiVariant activeVariant = plainVariant(CUBE_GLOW_TEMPLATE.create(
					ctx.get(),
					new TextureMapping()
							.put(TextureSlot.ALL, TextureMapping.getBlockTexture(ctx.get()))
							.put(GLOW_SLOT, prov.modLoc("block/trivia_glow")),
					prov.modelOutput
			));
			MultiVariant inactiveVariant = plainVariant(ModelTemplates.CUBE_ALL.create(
					ModelLocationUtils.getModelLocation(ctx.get(), "_inactive"),
					useInactiveTexture ? TextureMapping.cube(prov.modLoc("block/trivia_inactive")) : TextureMapping.cube(ctx.get()),
					prov.modelOutput
			));
			prov.blockStateOutput.accept(MultiVariantGenerator.dispatch(ctx.get())
					.with(createBooleanModelDispatch(TriviaBlock.ANSWERED, inactiveVariant, activeVariant)));
		}

		private static void generateChestItem(DataGenContext<Item, BlockItem> ctx, RegistrateItemModelGenerator prov, ResourceLocation texture, ResourceLocation particle) {
			ResourceLocation baseModel = ModelTemplates.CHEST_INVENTORY.create(ctx.get(), TextureMapping.particle(particle), prov.modelOutput);
			prov.itemModelOutput.accept(ctx.get(), ItemModelUtils.specialModel(baseModel, new ChestSpecialRenderer.Unbaked(texture)));
		}
	}
}
