package org.lovetropics.games.common.content.crafting_bee;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

public interface RecipeSelector {
	BiMap<String, MapCodec<? extends RecipeSelector>> TYPES = ImmutableBiMap.of("from_list", FromList.CODEC, "one_of", OneOf.CODEC, "from_item_tag", FromItemTag.CODEC);
	Codec<RecipeSelector> CODEC = Codec.STRING.dispatch(s -> TYPES.inverse().get(s.getType()), TYPES::get);

	RecipeHolder<?> select(RecipeManager recipes, RegistryAccess registryAccess, RandomSource random);

	MapCodec<? extends RecipeSelector> getType();

	record FromList(List<ResourceKey<Recipe<?>>> recipes) implements RecipeSelector {
		public static final MapCodec<FromList> CODEC = ResourceKey.codec(Registries.RECIPE).listOf().fieldOf("recipes")
				.xmap(FromList::new, FromList::recipes);

		private static final Logger LOGGER = LogUtils.getLogger();

		@Override
		public RecipeHolder<?> select(RecipeManager recipes, RegistryAccess registryAccess, RandomSource random) {
			Optional<RecipeHolder<?>> recipe = Optional.empty();
			while (recipe.isEmpty()) {
				ResourceKey<Recipe<?>> key = Util.getRandom(this.recipes, random);
				recipe = recipes.byKey(key);
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
		public RecipeHolder<?> select(RecipeManager recipes, RegistryAccess registryAccess, RandomSource random) {
			return Util.getRandom(selectors, random).select(recipes, registryAccess, random);
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

		private @Nullable List<RecipeHolder<?>> cache;

		public FromItemTag(TagKey<Item> tag) {
			this.tag = tag;
		}

		@Override
		public RecipeHolder<?> select(RecipeManager recipes, RegistryAccess registryAccess, RandomSource random) {
			if (cache == null) {
				cache = recipes.getRecipes().stream()
						.filter(h -> {
							ItemStack result = CraftingBee.getCraftingRecipeResult(h.value(), registryAccess);
							return result.is(tag) && isVanilla(h);
						})
						.toList();
			}
			return Util.getRandom(cache, random);
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
