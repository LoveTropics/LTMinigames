package org.lovetropics.games.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.lovetropics.games.client.game.ClientGameStateManager;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import net.minecraft.client.ScrollWheelHandler;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ScrollWheelHandler.class)
public class ScrollWheelHandlerMixin {

	@WrapOperation(method = "onMouseScroll", at = @At(value = "NEW", target = "(II)Lorg/joml/Vector2i;"))
	public Vector2i onOnMouseScroll(int x, int y, Operation<Vector2i> original) {
		if (ClientGameStateManager.isSet(GameClientStateTypes.INVERT_SCROLL_WHEEL)) {
			return new Vector2i(-x, -y);
		}
		return original.call(x, y);
	}
}
