package com.lovetropics.minigames.mixin.client;

import com.lovetropics.minigames.client.game.ClientGameStateManager;
import com.lovetropics.minigames.common.content.turtle_race.RiderBehavior;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin extends ClientInput {
	@Inject(method = "tick", at = @At("TAIL"))
	private void respectMovementRules(CallbackInfo ci) {
		if (ClientGameStateManager.getOrNull(GameClientStateTypes.DISABLE_PLAYER_MOVEMENT) != null) {
			keyPresses = new Input(
					false,
					false,
					false,
					false,
					false,
					keyPresses.shift(),
					false
			);
			moveVector = Vec2.ZERO;
			return;
		}
		if (ClientGameStateManager.getOrNull(GameClientStateTypes.SWAP_MOVEMENT) != null) {
			keyPresses = new Input(
					keyPresses.left(),
					keyPresses.right(),
					keyPresses.forward(),
					keyPresses.backward(),
					keyPresses.jump(),
					keyPresses.shift(),
					keyPresses.sprint()
			);
			moveVector = new Vec2(moveVector.y, moveVector.x);
		}
		if (Minecraft.getInstance().player.getExistingData(RiderBehavior.FORCE_RIDER).orElse(false)) {
			keyPresses = new Input(
					keyPresses.forward(),
					keyPresses.backward(),
					keyPresses.left(),
					keyPresses.right(),
					keyPresses.jump(),
					false,
					keyPresses.sprint()
			);
		}
	}
}
