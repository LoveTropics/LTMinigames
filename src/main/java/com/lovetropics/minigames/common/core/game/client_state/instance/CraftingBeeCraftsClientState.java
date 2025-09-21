package com.lovetropics.minigames.common.core.game.client_state.instance;

import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateType;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;

import java.util.List;
import java.util.UUID;

public record CraftingBeeCraftsClientState(List<Craft> crafts, UUID gameId, int allowedHints) implements GameClientState {
	public static final MapCodec<CraftingBeeCraftsClientState> CODEC = RecordCodecBuilder.mapCodec(in -> in.group(
			Craft.CODEC.listOf().fieldOf("crafts").forGetter(CraftingBeeCraftsClientState::crafts),
			UUIDUtil.CODEC.fieldOf("gameId").forGetter(CraftingBeeCraftsClientState::gameId),
			Codec.INT.fieldOf("allowedHints").forGetter(CraftingBeeCraftsClientState::allowedHints)
	).apply(in, CraftingBeeCraftsClientState::new));

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.CRAFTING_BEE_CRAFTS.get();
	}

	public record Craft(ItemStack output, ResourceKey<Recipe<?>> recipeId, RecipeDisplay display, boolean done) {
		public static final Codec<Craft> CODEC = RecordCodecBuilder.create(in -> in.group(
				ItemStack.CODEC.fieldOf("output").forGetter(Craft::output),
				ResourceKey.codec(Registries.RECIPE).fieldOf("recipe").forGetter(Craft::recipeId),
				RecipeDisplay.CODEC.fieldOf("display").forGetter(Craft::display),
				Codec.BOOL.optionalFieldOf("done", false).forGetter(Craft::done)
		).apply(in, Craft::new));
	}
}
