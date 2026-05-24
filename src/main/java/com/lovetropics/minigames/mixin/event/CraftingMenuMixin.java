package com.lovetropics.minigames.mixin.event;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.impl.GamePhaseManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CraftingMenu.class)
public class CraftingMenuMixin {

	@WrapOperation(method = "slotChangedCraftingGrid", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/CraftingRecipe;assemble(Lnet/minecraft/world/item/crafting/RecipeInput;)Lnet/minecraft/world/item/ItemStack;"))
	private static ItemStack modifyResult(CraftingRecipe instance, RecipeInput recipeInput, Operation<ItemStack> original, @Local(name = "serverPlayer") ServerPlayer serverPlayer) {
		ItemStack originalResult = original.call(instance, recipeInput);
		IGamePhase game = GamePhaseManager.get().getGamePhaseFor(serverPlayer);
		if (game != null) {
			return game.invoker(GamePlayerEvents.CRAFT_RESULT).modifyResult(serverPlayer, originalResult, (CraftingInput) recipeInput, instance);
		}
		return originalResult;
	}
}
