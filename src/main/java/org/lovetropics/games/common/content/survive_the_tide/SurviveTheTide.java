package org.lovetropics.games.common.content.survive_the_tide;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.generators.RegistrateBlockModelGenerator;
import com.tterrag.registrate.providers.generators.RegistrateItemModelGenerator;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.client.render.block.BigRedButtonBlockEntityRenderer;
import org.lovetropics.games.client.render.entity.DriftwoodRenderer;
import org.lovetropics.games.client.render.entity.LightningArrowRenderer;
import org.lovetropics.games.client.render.entity.PlatformRenderer;
import org.lovetropics.games.common.content.survive_the_tide.behavior.FormIcebergsGameBehavior;
import org.lovetropics.games.common.content.survive_the_tide.behavior.PhasedWeatherControlBehavior;
import org.lovetropics.games.common.content.survive_the_tide.behavior.RevealPlayersBehavior;
import org.lovetropics.games.common.content.survive_the_tide.behavior.RisingPlatformBehavior;
import org.lovetropics.games.common.content.survive_the_tide.behavior.SttChatBroadcastBehavior;
import org.lovetropics.games.common.content.survive_the_tide.behavior.SttSidebarBehavior;
import org.lovetropics.games.common.content.survive_the_tide.behavior.SttWinLogicBehavior;
import org.lovetropics.games.common.content.survive_the_tide.behavior.SurviveTheTideRulesetBehavior;
import org.lovetropics.games.common.content.survive_the_tide.behavior.SurviveTheTideWeatherControlBehavior;
import org.lovetropics.games.common.content.survive_the_tide.behavior.SurviveTheTideWindController;
import org.lovetropics.games.common.content.survive_the_tide.behavior.WorldBorderGameBehavior;
import org.lovetropics.games.common.content.survive_the_tide.block.BaseBigRedButtonBlock;
import org.lovetropics.games.common.content.survive_the_tide.block.BigRedButtonBlock;
import org.lovetropics.games.common.content.survive_the_tide.block.BigRedButtonBlockEntity;
import org.lovetropics.games.common.content.survive_the_tide.block.LootDispenserBlock;
import org.lovetropics.games.common.content.survive_the_tide.block.LootDispenserBlockEntity;
import org.lovetropics.games.common.content.survive_the_tide.entity.DriftwoodEntity;
import org.lovetropics.games.common.content.survive_the_tide.entity.LightningArrowEntity;
import org.lovetropics.games.common.content.survive_the_tide.entity.PlatformEntity;
import org.lovetropics.games.common.content.survive_the_tide.item.AcidRepellentUmbrellaItem;
import org.lovetropics.games.common.content.survive_the_tide.item.LightningArrowItem;
import org.lovetropics.games.common.content.survive_the_tide.item.PaddleItem;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;

import java.util.List;

import static net.minecraft.client.data.models.BlockModelGenerators.plainVariant;
import static net.minecraft.world.level.storage.loot.LootTable.lootTable;

public final class SurviveTheTide {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final BlockEntry<BigRedButtonBlock> BIG_RED_BUTTON = REGISTRATE
			.block("big_red_button", BigRedButtonBlock::new)
			.initialProperties(() -> Blocks.STONE_BUTTON)
			.properties(BlockBehaviour.Properties::noLootTable)
			.blockEntity(BigRedButtonBlockEntity::new)
			.renderer(() -> BigRedButtonBlockEntityRenderer::new)
			.build()
			.blockstate(() -> Models::generateCustomButton)
			.simpleItem()
			.register();

	public static final BlockEntry<BaseBigRedButtonBlock> NORMAL_BIG_RED_BUTTON = REGISTRATE
			.block("normal_big_red_button", BaseBigRedButtonBlock::new)
			.initialProperties(() -> Blocks.STONE_BUTTON)
			.blockstate(() -> (ctx, prov) -> Models.generateCustomButton(ctx, prov, BIG_RED_BUTTON.get()))
			.item()
			.model(() -> (ctx, prov) -> Models.generateExistingModel(ctx, prov, BIG_RED_BUTTON.asItem()))
			.build()
			.register();

	public static final BlockEntityEntry<BigRedButtonBlockEntity> BIG_RED_BUTTON_ENTITY = BlockEntityEntry.cast(REGISTRATE.get("big_red_button", Registries.BLOCK_ENTITY_TYPE));

	public static final BlockEntry<LootDispenserBlock> LOOT_DISPENSER = REGISTRATE
			.block("loot_dispenser", LootDispenserBlock::new)
			.initialProperties(() -> Blocks.DISPENSER)
			.properties(BlockBehaviour.Properties::noLootTable)
			.blockEntity(LootDispenserBlockEntity::new).build()
			.blockstate(() -> Models::generateLootDispenser)
			.simpleItem()
			.register();

	public static final BlockEntityEntry<LootDispenserBlockEntity> LOOT_DISPENSER_ENTITY = BlockEntityEntry.cast(REGISTRATE.get("loot_dispenser", Registries.BLOCK_ENTITY_TYPE));

	public static final ItemEntry<Item> SUPER_SUNSCREEN = REGISTRATE.item("super_sunscreen", Item::new)
			.properties(p -> p.durability(180)
					.component(DataComponents.LORE, simpleLore(SurviveTheTideTexts.SUPER_SUNSCREEN_TOOLTIP)))
			.register();

	public static final ItemEntry<AcidRepellentUmbrellaItem> ACID_REPELLENT_UMBRELLA = REGISTRATE.item("acid_repellent_umbrella", AcidRepellentUmbrellaItem::new)
			.properties(p -> p.component(DataComponents.LORE, simpleLore(SurviveTheTideTexts.ACID_REPELLENT_UMBRELLA_TOOLTIP)))
			.model(() -> Models::generateExistingModel)
			.register();

	public static final ItemEntry<PaddleItem> PADDLE = REGISTRATE.item("paddle", PaddleItem::new)
			.properties(p -> p.component(DataComponents.LORE, simpleLore(SurviveTheTideTexts.PADDLE_TOOLTIP)))
			.model(() -> Models::generateExistingModel)
			.register();

	public static final ItemEntry<LightningArrowItem> LIGHTNING_ARROW = REGISTRATE.item("lightning_arrow", LightningArrowItem::new)
			.tag(ItemTags.ARROWS)
			.register();

	public static final RegistryEntry<EntityType<?>, EntityType<DriftwoodEntity>> DRIFTWOOD = REGISTRATE.entity("driftwood", DriftwoodEntity::new, MobCategory.MISC)
			.properties(properties -> properties.sized(2.0F, 1.0F).setShouldReceiveVelocityUpdates(true).setUpdateInterval(3))
			.loot((loot, type) -> loot.add(type, lootTable()))
			.renderer(() -> DriftwoodRenderer::new)
			.register();

	public static final RegistryEntry<EntityType<?>, EntityType<LightningArrowEntity>> LIGHTNING_ARROW_ENTITY = REGISTRATE.<LightningArrowEntity>entity("lightning_arrow", LightningArrowEntity::new, MobCategory.MISC)
			.properties(properties -> properties.sized(0.5f, 0.5f).clientTrackingRange(4).updateInterval(SharedConstants.TICKS_PER_SECOND))
			.loot((loot, type) -> loot.add(type, lootTable()))
			.renderer(() -> LightningArrowRenderer::new)
			.register();

	public static final RegistryEntry<EntityType<?>, EntityType<PlatformEntity>> PLATFORM = REGISTRATE.entity("platform", PlatformEntity::new, MobCategory.MISC)
			.properties(properties -> properties.sized(0.0f, 0.0f).updateInterval(3).clientTrackingRange(1))
			.loot((loot, type) -> loot.add(type, lootTable()))
			.renderer(() -> PlatformRenderer::new)
			.register();

	public static void init() {
		REGISTRATE.registerBehavior("form_icebergs", FormIcebergsGameBehavior.CODEC);
		REGISTRATE.registerBehavior("survive_the_tide_ruleset", SurviveTheTideRulesetBehavior.CODEC);
		REGISTRATE.registerBehavior("survive_the_tide_chat_broadcast", SttChatBroadcastBehavior.CODEC);
		REGISTRATE.registerBehavior("survive_the_tide_win_logic", SttWinLogicBehavior.CODEC);
		REGISTRATE.registerBehavior("reveal_players", RevealPlayersBehavior.CODEC);
		REGISTRATE.registerBehavior("world_border", WorldBorderGameBehavior.CODEC);
		REGISTRATE.registerBehavior("stt_weather_control", SurviveTheTideWeatherControlBehavior.CODEC);
		REGISTRATE.registerBehavior("stt_wind_control", SurviveTheTideWindController.CODEC);
		REGISTRATE.registerBehavior("phased_weather_control", PhasedWeatherControlBehavior.CODEC);
		REGISTRATE.registerBehavior("survive_the_tide_sidebar", SttSidebarBehavior.CODEC);
		REGISTRATE.registerBehavior("rising_platform", RisingPlatformBehavior.CODEC);
	}

	public static ItemLore simpleLore(Component lore) {
		return new ItemLore(List.of(lore.copy().withStyle(style -> {
			if (style.getColor() == null) {
				style = style.withColor(ChatFormatting.WHITE);
			}
			return style.withItalic(style.isItalic());
		})));
	}

	private static class Models {
		private static void generateCustomButton(DataGenContext<Block, ? extends ButtonBlock> ctx, RegistrateBlockModelGenerator prov) {
			generateCustomButton(ctx, prov, ctx.get());
		}

		private static void generateCustomButton(DataGenContext<Block, ? extends ButtonBlock> ctx, RegistrateBlockModelGenerator prov, ButtonBlock modelBlock) {
			prov.generateButtonBlock(ctx.get(),
					plainVariant(ModelLocationUtils.getModelLocation(modelBlock)),
					plainVariant(ModelLocationUtils.getModelLocation(modelBlock, "_pressed"))
			);
		}

		private static void generateLootDispenser(DataGenContext<Block, LootDispenserBlock> ctx, RegistrateBlockModelGenerator prov) {
			prov.generateDirectionalBlock(ctx.get(), plainVariant(ModelTemplates.CUBE_ORIENTABLE_VERTICAL.create(
					ModelLocationUtils.getModelLocation(ctx.get()),
					new TextureMapping()
							.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.FURNACE, "_side"))
							.put(TextureSlot.FRONT, TextureMapping.getBlockTexture(Blocks.DISPENSER, "_front_vertical")),
					prov.modelOutput
			)));
		}

		private static void generateExistingModel(DataGenContext<Item, ? extends Item> ctx, RegistrateItemModelGenerator prov) {
			generateExistingModel(ctx, prov, ctx.get());
		}

		private static void generateExistingModel(DataGenContext<Item, ? extends Item> ctx, RegistrateItemModelGenerator prov, Item modelItem) {
			prov.createWithExistingModel(ctx.get(), ModelLocationUtils.getModelLocation(modelItem));
		}
	}
}
