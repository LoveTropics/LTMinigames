package com.lovetropics.minigames.client.render.block;

import com.lovetropics.minigames.common.content.survive_the_tide.block.BigRedButtonBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BigRedButtonBlockEntityRenderer implements BlockEntityRenderer<BigRedButtonBlockEntity> {
	private static final int TEXT_PADDING = 4;
	private static final float Z_OFFSET = 0.01f;

	private static final int START_COLOR = ChatFormatting.GOLD.getColor();
	private static final int TRIGGERED_COLOR = ChatFormatting.GREEN.getColor();

	private final Font font;

	public BigRedButtonBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
		font = context.getFont();
	}

	@Override
	public void render(BigRedButtonBlockEntity entity, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, Vec3 cameraPos) {
		int presentCount = entity.getPlayersPresentCount();
		int requiredCount = entity.getPlayersRequiredCount();
		String text = presentCount + "/" + requiredCount;

		poseStack.pushPose();
		poseStack.translate(0.5f, 0.5f, 0.5f);

		BlockState state = entity.getBlockState();
		poseStack.mulPose(state.getValue(ButtonBlock.FACING).getRotation());
		switch (state.getValue(ButtonBlock.FACE)) {
			case WALL -> poseStack.mulPose(Axis.XP.rotation(Mth.PI / 2.0f));
			case CEILING -> poseStack.mulPose(Axis.XP.rotation(Mth.PI));
		}
		poseStack.translate(0.0f, -0.5f, 0.5f - Z_OFFSET);

		int textWidth = font.width(text);
		float scale = 1.0f / (textWidth + TEXT_PADDING);
		poseStack.scale(scale, scale, -scale);

		int color = ARGB.lerp((float) presentCount / requiredCount, START_COLOR, TRIGGERED_COLOR);
		font.drawInBatch(text, -textWidth / 2.0f, -font.lineHeight, color, true, poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);

		poseStack.popPose();
	}
}
