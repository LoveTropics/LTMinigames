package com.lovetropics.minigames.mixin.client;

import com.lovetropics.minigames.client.game.ClientGameStateManager;
import com.lovetropics.minigames.common.content.turtle_race.RiderBehavior;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.lovetropics.minigames.common.core.game.client_state.instance.controls.RemapMovementClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin extends ClientInput {
	@Shadow
	private static float calculateImpulse(boolean positive, boolean negative) {
		throw new UnsupportedOperationException("Implemented via mixin");
	}

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

		RemapMovementClientState remapState = ClientGameStateManager.getOrNull(GameClientStateTypes.REMAP_MOVEMENT);
		if (remapState != null) {
			keyPresses = remapState.remap(keyPresses);

			// Recalculate move vector to make sure it matches changes in movement.
			float forwardImpulse = calculateImpulse(this.keyPresses.forward(), this.keyPresses.backward());
			float leftImpulse = calculateImpulse(this.keyPresses.left(), this.keyPresses.right());
			this.moveVector = new Vec2(leftImpulse, forwardImpulse).normalized();
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
