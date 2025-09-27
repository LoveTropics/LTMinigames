package com.lovetropics.minigames.common.content.crafting_bee.ingredient;

import com.lovetropics.minigames.common.content.crafting_bee.CraftingBee;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class FromRecipeDecomposer implements IngredientDecomposer {
	public static final MapCodec<FromRecipeDecomposer> CODEC = ResourceKey.codec(Registries.RECIPE).listOf()
			.fieldOf("recipes").xmap(FromRecipeDecomposer::new, d -> d.recipes);

	private final List<ResourceKey<Recipe<?>>> recipes;

	private final Map<Item, List<Ingredient>> cache = new IdentityHashMap<>();

	public FromRecipeDecomposer(List<ResourceKey<Recipe<?>>> recipes) {
		this.recipes = recipes;
	}

	@Override
	public @Nullable List<Ingredient> decompose(Ingredient ingredient) {
		HolderSet<Item> values = ingredient.getValues();
		if (values.size() == 1) {
			return cache.get(ingredient.getValues().get(0).value());
		}
		return null;
	}

	@Override
	public void prepareCache(ServerLevel level) {
		cache.clear();

		for (ResourceKey<Recipe<?>> recipeId : recipes) {
			level.getServer().getRecipeManager().byKey(recipeId).ifPresent(holder -> {
				ItemStack result = CraftingBee.getCraftingRecipeResult(holder.value(), level.registryAccess());
				if (result.isEmpty()) {
					return;
				}
				if (holder.value() instanceof ShapedRecipe shapedRecipe) {
					cache.put(result.getItem(), shapedRecipe.placementInfo().ingredients());
				} else if (holder.value() instanceof ShapelessRecipe shapelessRecipe) {
					cache.put(result.getItem(), shapelessRecipe.placementInfo().ingredients());
				}
			});
		}
	}

	@Override
	public MapCodec<? extends IngredientDecomposer> codec() {
		return CODEC;
	}
}
