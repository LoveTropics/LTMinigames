package com.lovetropics.minigames.common.content.crafting_bee;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public interface RecipeSelector {
	BiMap<String, MapCodec<? extends RecipeSelector>> TYPES = ImmutableBiMap.of("from_list", FromList.CODEC, "one_of", OneOf.CODEC, "from_item_tag", FromItemTag.CODEC);
	Codec<RecipeSelector> CODEC = Codec.STRING.dispatch(s -> TYPES.inverse().get(s.getType()), TYPES::get);

	RecipeHolder<?> select(ServerLevel level);

	MapCodec<? extends RecipeSelector> getType();

	record FromList(List<ResourceKey<Recipe<?>>> recipes) implements RecipeSelector {
		public static final MapCodec<FromList> CODEC = ResourceKey.codec(Registries.RECIPE).listOf().fieldOf("recipes")
				.xmap(FromList::new, FromList::recipes);

		private static final Logger LOGGER = LogUtils.getLogger();

		@Override
		public RecipeHolder<?> select(ServerLevel level) {
			Optional<RecipeHolder<?>> recipe = Optional.empty();
			while (recipe.isEmpty()) {
				var key = Util.getRandom(recipes, level.getRandom());
				recipe = level.recipeAccess().byKey(key);
				if (recipe.isEmpty()) {
					LOGGER.error("Recipe '{}' doesn't exist", key);
				}
			}
			return recipe.get();
		}

		@Override
		public MapCodec<? extends RecipeSelector> getType() {
			return CODEC;
		}
	}

	record OneOf(List<RecipeSelector> selectors) implements RecipeSelector {
		public static final MapCodec<OneOf> CODEC = MapCodec.assumeMapUnsafe(Codec.lazyInitialized(() -> RecipeSelector.CODEC.listOf().fieldOf("selectors")
				.xmap(OneOf::new, OneOf::selectors).codec()));

		@Override
		public RecipeHolder<?> select(ServerLevel level) {
			return Util.getRandom(selectors, level.getRandom()).select(level);
		}

		@Override
		public MapCodec<? extends RecipeSelector> getType() {
			return CODEC;
		}
	}

	class FromItemTag implements RecipeSelector {
		public static final MapCodec<FromItemTag> CODEC = TagKey.hashedCodec(Registries.ITEM).fieldOf("tag")
				.xmap(FromItemTag::new, s -> s.tag);

		private final TagKey<Item> tag;

		@Nullable
		private List<RecipeHolder<?>> cache;

		public FromItemTag(TagKey<Item> tag) {
			this.tag = tag;
		}

		@Override
		public RecipeHolder<?> select(ServerLevel level) {
			if (cache == null) {
				cache = level.recipeAccess().getRecipes().stream()
						.filter(h -> {
							ItemStack result = CraftingBee.getCraftingRecipeResult(h.value(), level.registryAccess());
							return result.is(tag) && isVanilla(h);
						})
						.toList();
			}
			return Util.getRandom(cache, level.getRandom());
		}

		@Override
		public MapCodec<? extends RecipeSelector> getType() {
			return CODEC;
		}
	}

	private static boolean isVanilla(RecipeHolder<?> recipe) {
		return recipe.id().identifier().getNamespace().equals(Identifier.DEFAULT_NAMESPACE);
	}
}
