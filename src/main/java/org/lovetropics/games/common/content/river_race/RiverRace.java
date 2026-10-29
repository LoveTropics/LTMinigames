package org.lovetropics.games.common.content.river_race;

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
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.content.river_race.behaviour.CollectablesBehaviour;
import org.lovetropics.games.common.content.river_race.behaviour.KillAboveVoidBehavior;
import org.lovetropics.games.common.content.river_race.behaviour.ModifyMaxSpawnsAction;
import org.lovetropics.games.common.content.river_race.behaviour.ProgressBehaviour;
import org.lovetropics.games.common.content.river_race.behaviour.RewardsFromMicrogameBehavior;
import org.lovetropics.games.common.content.river_race.behaviour.RiverRaceMerchantBehavior;
import org.lovetropics.games.common.content.river_race.behaviour.RiverRaceSetupBehavior;
import org.lovetropics.games.common.content.river_race.behaviour.RiverRaceSpawnsBehavior;
import org.lovetropics.games.common.content.river_race.behaviour.RiverRaceZoneBehavior;
import org.lovetropics.games.common.content.river_race.behaviour.TriviaBehaviour;
import org.lovetropics.games.common.content.river_race.behaviour.UnlockZoneAction;
import org.lovetropics.games.common.content.river_race.behaviour.VictoryPointsBehavior;
import org.lovetropics.games.common.content.river_race.block.TriviaBlock;
import org.lovetropics.games.common.content.river_race.block.TriviaBlockEntity;
import org.lovetropics.games.common.content.river_race.block.TriviaChestBlock;
import org.lovetropics.games.common.content.river_race.block.TriviaChestBlockEntity;
import org.lovetropics.games.common.content.river_race.client_state.RiverRaceClientBarState;
import org.lovetropics.games.common.util.registry.GameClientTweakEntry;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;

import static net.minecraft.client.data.models.BlockModelGenerators.createBooleanModelDispatch;
import static net.minecraft.client.data.models.BlockModelGenerators.plainVariant;

public class RiverRace {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, LoveTropics.ID);

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
					Models.generateChestItem(ctx, prov, LoveTropics.id("trivia"), TextureMapping.getBlockTexture(TRIVIA_VICTORY.get()))
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
		REGISTRATE.registerBehavior("river_race/setup", RiverRaceSetupBehavior.CODEC);
		REGISTRATE.registerBehavior("river_race/zone", RiverRaceZoneBehavior.CODEC);
		REGISTRATE.registerBehavior("river_race/spawns", RiverRaceSpawnsBehavior.CODEC);
		REGISTRATE.registerBehavior("river_race/modify_max_spawns", ModifyMaxSpawnsAction.CODEC);
		REGISTRATE.registerBehavior("trivia", TriviaBehaviour.CODEC);
		REGISTRATE.registerBehavior("victory_points", VictoryPointsBehavior.CODEC);
		REGISTRATE.registerBehavior("river_race_merchant", RiverRaceMerchantBehavior.CODEC);
		REGISTRATE.registerBehavior("river_race_progress", ProgressBehaviour.CODEC);
		REGISTRATE.registerBehavior("river_race_collectables", CollectablesBehaviour.CODEC);
		REGISTRATE.registerBehavior("unlock_zone", UnlockZoneAction.CODEC);
		REGISTRATE.registerBehavior("kill_above_void", KillAboveVoidBehavior.CODEC);
		REGISTRATE.registerBehavior("rewards_from_microgame", RewardsFromMicrogameBehavior.CODEC);
	}

	private static class Models {
		private static final TextureSlot GLOW_SLOT = TextureSlot.create("glow");

		private static final ModelTemplate CUBE_GLOW_TEMPLATE = ModelTemplates.create(LoveTropics.id("cube_glow").toString(), TextureSlot.ALL, GLOW_SLOT);

		private static void generateTriviaBlock(DataGenContext<Block, ?> ctx, RegistrateBlockModelGenerator prov, boolean useInactiveTexture) {
			MultiVariant activeVariant = plainVariant(CUBE_GLOW_TEMPLATE.create(
					ctx.get(),
					new TextureMapping()
							.put(TextureSlot.ALL, TextureMapping.getBlockTexture(ctx.get()))
							.put(GLOW_SLOT, prov.modBlockTexture("trivia_glow")),
					prov.modelOutput
			));
			MultiVariant inactiveVariant = plainVariant(ModelTemplates.CUBE_ALL.create(
					ModelLocationUtils.getModelLocation(ctx.get(), "_inactive"),
					useInactiveTexture ? TextureMapping.cube(prov.modBlockTexture("trivia_inactive")) : TextureMapping.cube(ctx.get()),
					prov.modelOutput
			));
			prov.blockStateOutput.accept(MultiVariantGenerator.dispatch(ctx.get())
					.with(createBooleanModelDispatch(TriviaBlock.ANSWERED, inactiveVariant, activeVariant)));
		}

		private static void generateChestItem(DataGenContext<Item, BlockItem> ctx, RegistrateItemModelGenerator prov, Identifier texture, Material particle) {
			Identifier baseModel = ModelTemplates.CHEST_INVENTORY.create(ctx.get(), TextureMapping.particle(particle), prov.modelOutput);
			prov.itemModelOutput.accept(ctx.get(), ItemModelUtils.specialModel(baseModel, new ChestSpecialRenderer.Unbaked(texture)));
		}
	}
}
