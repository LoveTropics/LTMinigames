package org.lovetropics.games.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.lovetropics.games.client.game.ClientGameStateManager;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import org.lovetropics.games.common.core.game.client_state.instance.DisableRecipeBookClientState;
import org.lovetropics.games.common.core.game.client_state.instance.HideRecipeBookClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.RecipeBookType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractRecipeBookScreen.class)
public class AbstractRecipeBookScreenMixin {
	@Inject(method = "init", at = @At("HEAD"))
	private void hideBookIfOpen(CallbackInfo ci) {
		if (ClientGameStateManager.getOrNull(GameClientStateTypes.DISABLE_RECIPE_BOOK) != null) {
			Minecraft.getInstance().player.getRecipeBook().setBookSetting(RecipeBookType.CRAFTING, false, false);
		}
	}

	@WrapOperation(method = "initButton", at = @At(value = "NEW", target = "net/minecraft/client/gui/components/ImageButton"))
	private ImageButton respectHiddenBook(int x, int y, int width, int height, WidgetSprites sprites, Button.OnPress onPress, Operation<ImageButton> original) {
		Identifier disabled = Identifier.fromNamespaceAndPath("ltminigames", "recipe_book/button_disabled");
		ImageButton org = original.call(x, y, width, height, new WidgetSprites(
				sprites.enabled(), disabled, sprites.enabledFocused(), disabled
		), onPress);
		DisableRecipeBookClientState hidden = ClientGameStateManager.getOrNull(GameClientStateTypes.DISABLE_RECIPE_BOOK);
		if (hidden != null) {
			org.active = false;
			org.setTooltip(Tooltip.create(hidden.message()));
		}
		return org;
	}

	@Inject(method = "initButton", at = @At("HEAD"), cancellable = true)
	private void onInitButton(CallbackInfo ci) {
		HideRecipeBookClientState state = ClientGameStateManager.getOrNull(GameClientStateTypes.HIDE_RECIPE_BOOK);
		if (state != null) {
			ci.cancel();
		}
	}
}
