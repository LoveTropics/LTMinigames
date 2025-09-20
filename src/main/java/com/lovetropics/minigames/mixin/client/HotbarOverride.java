package com.lovetropics.minigames.mixin.client;

import com.lovetropics.minigames.client.game.ClientGameStateManager;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.lovetropics.minigames.common.core.game.client_state.instance.ReplaceTexturesClientState;
import net.minecraft.client.gui.Gui;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Gui.class)
public class HotbarOverride {
	@Unique
	private static final ResourceLocation HOTBAR_SPRITE = ResourceLocation.withDefaultNamespace("hud/hotbar");

	@ModifyArg(
			method = "renderItemHotbar",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIII)V"),
			index = 1)
	public ResourceLocation getHotbarTexture(ResourceLocation sprite) {
		if (!sprite.equals(HOTBAR_SPRITE)) {
			return sprite;
		}
		ReplaceTexturesClientState textures = ClientGameStateManager.getOrNull(GameClientStateTypes.REPLACE_TEXTURES);
		if (textures != null) {
			ResourceLocation hotbar = textures.getTexture(ReplaceTexturesClientState.TextureType.HOTBAR);
			if (hotbar != null) {
				return hotbar;
			}
		}
		return sprite;
	}
}
