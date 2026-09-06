package com.lovetropics.minigames.common.content.crafting_bee.ingredient;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface IngredientDecomposer {
	BiMap<String, MapCodec<? extends IngredientDecomposer>> TYPES = ImmutableBiMap.of("prefer_from_tag", PreferItemFromTagDecomposer.CODEC,
			"from_recipe", FromRecipeDecomposer.CODEC,
			"simple_tag", SimpleTagToItemDecomposer.CODEC);
	MapCodec<IngredientDecomposer> CODEC = Codec.STRING.dispatchMap(s -> TYPES.inverse().get(s.codec()), TYPES::get);

	@Nullable List<Ingredient> decompose(Ingredient ingredient);

	default void prepareCache(ServerLevel level) {
	}

	MapCodec<? extends IngredientDecomposer> codec();
}
