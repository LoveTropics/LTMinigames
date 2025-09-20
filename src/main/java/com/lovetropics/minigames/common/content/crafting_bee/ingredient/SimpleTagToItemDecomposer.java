package com.lovetropics.minigames.common.content.crafting_bee.ingredient;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderSet;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record SimpleTagToItemDecomposer() implements IngredientDecomposer {
    public static final MapCodec<SimpleTagToItemDecomposer> CODEC = MapCodec.unit(SimpleTagToItemDecomposer::new);

    @Override
    public @Nullable List<Ingredient> decompose(Ingredient ingredient) {
        // This is a "hack". Neo will sometimes replace a vanilla recipe with a difference ingredient (#chests - #chests/trapped)
        // we just resolve it and return the first item
        if (ingredient.getCustomIngredient() != null || ingredient.getValues().size() == 1) {
            return List.of(Ingredient.of(HolderSet.direct(ingredient.getValues().get(0))));
        }
        return null;
    }

    @Override
    public MapCodec<? extends IngredientDecomposer> codec() {
        return CODEC;
    }
}
