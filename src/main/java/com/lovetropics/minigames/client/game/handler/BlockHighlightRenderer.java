package com.lovetropics.minigames.client.game.handler;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.game.ClientGameStateManager;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.lovetropics.minigames.common.core.game.client_state.instance.HighlightBlocksState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

import java.util.List;

@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public final class BlockHighlightRenderer {
	private static final int COLOR = ARGB.opaque(TextColor.AQUA.getValue());

	private static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();

	private static final BlockModelRenderState BLOCK_MODEL = new BlockModelRenderState();

	@SubscribeEvent
	public static void onRenderLevel(SubmitCustomGeometryEvent event) {
		HighlightBlocksState state = ClientGameStateManager.getOrNull(GameClientStateTypes.HIGHLIGHT_BLOCKS);
		if (state == null || state.positions().isEmpty()) {
			return;
		}

		List<BlockPos> positions = state.positions();

		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		Camera camera = minecraft.gameRenderer.mainCamera();
		if (level == null || !camera.isInitialized()) {
			return;
		}

		minecraft.getBlockModelResolver().update(BLOCK_MODEL, Blocks.STONE.defaultBlockState(), BLOCK_DISPLAY_CONTEXT);
		if (BLOCK_MODEL.isEmpty()) {
			return;
		}

		Vec3 cameraPosition = camera.position();
		PoseStack poseStack = event.getPoseStack();

		for (BlockPos position : positions) {
			poseStack.pushPose();
			poseStack.translate(position.getX() - cameraPosition.x, position.getY() - cameraPosition.y, position.getZ() - cameraPosition.z);
			BLOCK_MODEL.submitOnlyOutline(poseStack, event.getSubmitNodeCollector(), LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, COLOR);
			poseStack.popPose();
		}
	}
}
