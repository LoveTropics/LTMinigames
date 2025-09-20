package com.lovetropics.minigames.common.core.item;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.render.special.MobItemSpecialRenderer;
import com.lovetropics.minigames.common.core.diguise.DisguiseType;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.generators.RegistrateItemModelGenerator;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.Util;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.Optional;

public class MinigameItems {
    
    private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();
 
    public static final ItemEntry<EditRegionItem> EDIT_REGION = REGISTRATE.item("edit_region", EditRegionItem::new)
            .register();

    public static final ItemEntry<DisguiseItem> DISGUISE = REGISTRATE.item("disguise", DisguiseItem::new)
            .properties(p -> p.stacksTo(1))
            .model(() -> Models::generateDisguiseItem)
            .addMiscData(ProviderType.LANG, prov -> {
                String descriptionId = Util.makeDescriptionId("item", LoveTropics.location("disguise"));
                prov.add(descriptionId + ".entity", "%s Disguise");
            })
            .tab(LoveTropics.TAB_KEY, modifier -> {
                for (EntityType<?> entity : BuiltInRegistries.ENTITY_TYPE) {
                    if (entity.getCategory() != MobCategory.MISC) {
                        final ItemStack stack = new ItemStack(MinigameItems.DISGUISE.get());
                        stack.set(MinigameDataComponents.DISGUISE, DisguiseType.DEFAULT.withEntity(new DisguiseType.EntityConfig(entity, null, false)));
                        modifier.accept(stack);
                    }
                }
            })
            .register();

    public static final ItemEntry<MobHatItem> MOB_HAT = REGISTRATE.item("mob_hat", MobHatItem::new)
            .properties(p -> p.stacksTo(1))
            .model(() -> Models::generateMobHatItem)
            .addMiscData(ProviderType.LANG, prov -> {
                String descriptionId = Util.makeDescriptionId("item", LoveTropics.location("mob_hat"));
                prov.add(descriptionId + ".entity", "%s Hat");
            })
            .tab(LoveTropics.TAB_KEY, modifier -> {
                for (EntityType<?> entity : BuiltInRegistries.ENTITY_TYPE) {
                    if (entity.getCategory() != MobCategory.MISC) {
                        final ItemStack stack = new ItemStack(MinigameItems.MOB_HAT.get());
                        stack.set(MinigameDataComponents.ENTITY, new DisguiseType.EntityConfig(entity, null, false));
                        modifier.accept(stack);
                    }
                }
            })
            .register();

    public static final ItemEntry<PlushieItem> PLUSHIE = REGISTRATE.item("plushie", PlushieItem::new)
            .properties(p -> p.stacksTo(1))
			.model(() -> Models::generatePlushieItem)
            .addMiscData(ProviderType.LANG, prov -> {
                String descriptionId = Util.makeDescriptionId("item", LoveTropics.location("plushie"));
                prov.add(descriptionId + ".entity", "%s Plushie");
            })
            .register();

    public static void init() {}

	private static class Models {
		private static final ResourceLocation DISGUISE_ITEM_SPRITE = LoveTropics.location("item/disguise");
		private static final ResourceLocation MOB_HAT_SPRITE = LoveTropics.location("item/mob_hat");

		private static void generateDisguiseItem(DataGenContext<Item, ? extends Item> ctx, RegistrateItemModelGenerator prov) {
			generateMobItem(ctx, prov, MobItemSpecialRenderer.EntitySource.DISGUISE, Optional.of(DISGUISE_ITEM_SPRITE));
		}

		private static void generateMobHatItem(DataGenContext<Item, ? extends Item> ctx, RegistrateItemModelGenerator prov) {
			generateMobItem(ctx, prov, MobItemSpecialRenderer.EntitySource.ENTITY, Optional.of(MOB_HAT_SPRITE));
		}

		private static void generatePlushieItem(DataGenContext<Item, ? extends Item> ctx, RegistrateItemModelGenerator prov) {
			generateMobItem(ctx, prov, MobItemSpecialRenderer.EntitySource.ENTITY, Optional.empty());
		}

		private static void generateMobItem(DataGenContext<Item, ? extends Item> ctx, RegistrateItemModelGenerator prov, MobItemSpecialRenderer.EntitySource entitySource, Optional<ResourceLocation> inventorySprite) {
			ResourceLocation baseModel = ModelTemplates.PARTICLE_ONLY.create(ctx.get(), TextureMapping.particle(Blocks.BLACK_WOOL), prov.modelOutput);
			prov.itemModelOutput.accept(ctx.get(), ItemModelUtils.specialModel(baseModel, new MobItemSpecialRenderer.Unbaked(entitySource, inventorySprite)));
		}
	}
}
