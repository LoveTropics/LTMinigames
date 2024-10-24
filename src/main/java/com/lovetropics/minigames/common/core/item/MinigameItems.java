package com.lovetropics.minigames.common.core.item;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.diguise.DisguiseType;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.generators.ModelFile;

public class MinigameItems {
    
    private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();
 
    public static final ItemEntry<EditRegionItem> EDIT_REGION = REGISTRATE.item("edit_region", EditRegionItem::new)
            .register();

    public static final ItemEntry<DisguiseItem> DISGUISE = REGISTRATE.item("disguise", DisguiseItem::new)
            .properties(p -> p.stacksTo(1))
            .model((ctx, prov) -> prov.getBuilder(ctx.getName()).parent(new ModelFile.UncheckedModelFile("builtin/entity")))
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
            .model((ctx, prov) -> prov.getBuilder(ctx.getName()).parent(new ModelFile.UncheckedModelFile("builtin/entity")))
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
            .model((ctx, prov) -> prov.getBuilder(ctx.getName()).parent(new ModelFile.UncheckedModelFile("builtin/entity")))
            .addMiscData(ProviderType.LANG, prov -> {
                String descriptionId = Util.makeDescriptionId("item", LoveTropics.location("plushie"));
                prov.add(descriptionId + ".entity", "%s Plushie");
            })
            .register();

    public static void init() {}
}
