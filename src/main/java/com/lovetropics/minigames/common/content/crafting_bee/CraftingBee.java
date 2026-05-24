package com.lovetropics.minigames.common.content.crafting_bee;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.util.registry.GameBehaviorEntry;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.function.Supplier;

public class CraftingBee {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final GameBehaviorEntry<CraftingBeeBehavior> CRAFTING_BEE = REGISTRATE.object("crafting_bee")
			.behavior(CraftingBeeBehavior.CODEC)
			.register();

	public static final Supplier<DataComponentType<CraftedUsing>> CRAFTED_USING = REGISTRATE.simple(
			"crafted_using",
			Registries.DATA_COMPONENT_TYPE,
			() -> DataComponentType.<CraftedUsing>builder()
					.networkSynchronized(CraftedUsing.STREAM_CODEC)
					.persistent(CraftedUsing.CODEC)
					.cacheEncoding()
					.build()
	);

	public static void init() {
	}

	public static ItemStack getCraftingRecipeResult(Recipe<?> recipe, HolderLookup.Provider registries) {
		if (!(recipe instanceof CraftingRecipe craftingRecipe)) {
			return ItemStack.EMPTY;
		}
		if (recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe) {
			return craftingRecipe.assemble(CraftingInput.EMPTY);
		}
		return ItemStack.EMPTY;
	}
}
