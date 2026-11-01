package org.lovetropics.games.mixin.client;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import org.lovetropics.games.client.game.ClientGameStateManager;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import org.lovetropics.games.common.core.game.client_state.instance.RemapSpriteClientState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TextureAtlas.class)
public class TextureAtlasMixin {

	@ModifyVariable(method = "getSprite", at = @At(value = "HEAD"), argsOnly = true, name = "location")
	public Identifier onGetSprite(Identifier location) {
		RemapSpriteClientState state = ClientGameStateManager.getOrNull(GameClientStateTypes.REMAP_SPRITE);
		if (state == null) {
			return location;
		}
		Identifier remapTexture = state.getRemapTexture(location);
		return remapTexture != null ? remapTexture : location;
	}
}
