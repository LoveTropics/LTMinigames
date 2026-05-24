package com.lovetropics.minigames.client.render.block;

import com.lovetropics.minigames.client.render.block.state.BigRedButtonBlockEntityRenderState;
import com.lovetropics.minigames.common.content.survive_the_tide.block.BigRedButtonBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BigRedButtonBlockEntityRenderer implements BlockEntityRenderer<BigRedButtonBlockEntity, BigRedButtonBlockEntityRenderState> {
	private static final int TEXT_PADDING = 4;
	private static final float Z_OFFSET = 0.01f;

	private static final int START_COLOR = ChatFormatting.GOLD.getColor();
	private static final int TRIGGERED_COLOR = ChatFormatting.GREEN.getColor();

	private final Font font;

	public BigRedButtonBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
		font = context.font();
	}

	@Override
	public BigRedButtonBlockEntityRenderState createRenderState() {
		return new BigRedButtonBlockEntityRenderState();
	}

	@Override
	public void extractRenderState(BigRedButtonBlockEntity blockEntity, BigRedButtonBlockEntityRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		state.presentCount = blockEntity.getPlayersPresentCount();
		state.requiredCount = blockEntity.getPlayersRequiredCount();
		state.facing = blockEntity.getBlockState().getValue(ButtonBlock.FACING);
		state.face  = blockEntity.getBlockState().getValue(ButtonBlock.FACE);
	}

	@Override
	public void submit(BigRedButtonBlockEntityRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		int presentCount = renderState.presentCount;
		int requiredCount = renderState.requiredCount;
		Direction facing = renderState.facing;
		AttachFace face = renderState.face;
		String text = presentCount + "/" + requiredCount;

		poseStack.pushPose();
		poseStack.translate(0.5f, 0.5f, 0.5f);

		poseStack.mulPose(facing.getRotation());
		switch (face) {
			case WALL -> poseStack.mulPose(Axis.XP.rotation(Mth.PI / 2.0f));
			case CEILING -> poseStack.mulPose(Axis.XP.rotation(Mth.PI));
		}
		poseStack.translate(0.0f, -0.5f, 0.5f - Z_OFFSET);

		int textWidth = font.width(text);
		float scale = 1.0f / (textWidth + TEXT_PADDING);
		poseStack.scale(scale, scale, -scale);

		int color = ARGB.linearLerp((float) presentCount / requiredCount, START_COLOR, TRIGGERED_COLOR);
		submitNodeCollector.submitText(
				poseStack,
				-textWidth / 2.0f,
				-font.lineHeight,
				FormattedCharSequence.forward(text, Style.EMPTY),
				true,
				Font.DisplayMode.NORMAL,
				renderState.lightCoords,
				color,
				0,
				0);

		poseStack.popPose();
	}
}
