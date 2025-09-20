package com.lovetropics.minigames.mixin.client;

import com.lovetropics.minigames.client.game.ClientGameStateManager;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.lovetropics.minigames.common.core.game.client_state.instance.CollidersClientState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "pick(F)V", at = @At("RETURN"))
	private void pick(float partialTicks, CallbackInfo ci) {
		HitResult hitResult = minecraft.hitResult;
		if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
			return;
		}

		LocalPlayer player = minecraft.player;
		CollidersClientState colliders = ClientGameStateManager.getOrNull(GameClientStateTypes.COLLIDERS);
		if (colliders == null || player == null) {
			return;
		}

		Vec3 start = player.getEyePosition(partialTicks);
		double currentDistance = hitResult.getLocation().distanceTo(start);

		Vec3 clip = colliders.clip(start, start.add(player.getViewVector(partialTicks).scale(currentDistance)));
		if (clip != null) {
			minecraft.hitResult = BlockHitResult.miss(clip, Direction.UP, BlockPos.containing(clip));
		}
	}

	@Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
	private void bobView(PoseStack poseStack, float partialTicks, CallbackInfo ci) {
		if (ClientGameStateManager.getOrNull(GameClientStateTypes.DISABLE_BOBBING) != null) {
			ci.cancel();
		}
	}
}
