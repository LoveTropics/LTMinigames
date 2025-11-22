package com.lovetropics.minigames.client.game.handler;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.game.ClientGameStateManager;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.lovetropics.minigames.common.core.game.client_state.instance.HighlightBlocksState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.List;

@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public final class BlockHighlightRenderer {
	private static final int COLOR = ChatFormatting.AQUA.getColor();

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent.AfterEntities event) {
		HighlightBlocksState state = ClientGameStateManager.getOrNull(GameClientStateTypes.HIGHLIGHT_BLOCKS);
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

		PoseStack poseStack = event.getPoseStack();

		OutlineBufferSource outlineBufferSource = Minecraft.getInstance().renderBuffers().outlineBufferSource();
		outlineBufferSource.setColor(ARGB.red(COLOR), ARGB.green(COLOR), ARGB.blue(COLOR), 0xff);
		for (BlockPos position : positions) {
			float x1 = (float) (position.getX() - camera.position().x);
			float y1 = (float) (position.getY() - camera.position().y);
			float z1 = (float) (position.getZ() - camera.position().z);

			// HAHAHA
			BlockStateModel blockModel = minecraft.getBlockRenderer().getBlockModel(Blocks.STONE.defaultBlockState());
			poseStack.pushPose();
			poseStack.translate(x1, y1, z1);
			ModelBlockRenderer.renderModel(
					poseStack.last(),
					outlineBufferSource.getBuffer(RenderType.outline(TextureAtlas.LOCATION_BLOCKS)),
					blockModel,
					0.0F,
					0.0F,
					0.0F,
					LightTexture.FULL_BRIGHT,
					OverlayTexture.NO_OVERLAY
			);
			poseStack.popPose();
		}
	}
}
