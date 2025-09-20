package com.lovetropics.minigames.client.game.handler;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.game.ClientGameStateManager;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.lovetropics.minigames.common.core.game.client_state.instance.BeaconClientState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.List;

@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public final class GameBeaconRenderer {
	private static final int COLOR = DyeColor.WHITE.getTextureDiffuseColor();

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent.AfterWeather event) {
		BeaconClientState state = ClientGameStateManager.getOrNull(GameClientStateTypes.BEACON);
		if (state == null || state.positions().isEmpty()) {
			return;
		}

		List<BlockPos> positions = state.positions();

		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		Camera camera = minecraft.gameRenderer.getMainCamera();
		if (level == null || !camera.isInitialized()) {
			return;
		}

		MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

		Vec3 cameraPosition = camera.getPosition();
		PoseStack poseStack = event.getPoseStack();

		long gameTime = level.getGameTime();
		float partialTick = event.getPartialTick().getGameTimeDeltaTicks();

		for (BlockPos position : positions) {
			poseStack.pushPose();
			poseStack.translate(position.getX() - cameraPosition.x, position.getY() - cameraPosition.y, position.getZ() - cameraPosition.z);
			BeaconRenderer.renderBeaconBeam(poseStack, bufferSource, BeaconRenderer.BEAM_LOCATION, partialTick, 0.0f, gameTime, 0, 256, COLOR, 0.15F, 0.175F);
			poseStack.popPose();
		}

		bufferSource.endBatch();
	}
}
