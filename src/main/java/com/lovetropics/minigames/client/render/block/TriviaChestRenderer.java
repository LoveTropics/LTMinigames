package com.lovetropics.minigames.client.render.block;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.river_race.block.TriviaChestBlock;
import com.lovetropics.minigames.common.content.river_race.block.TriviaChestBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class TriviaChestRenderer implements BlockEntityRenderer<TriviaChestBlockEntity> {
	private static final Material MATERIAL = new Material(Sheets.CHEST_SHEET, LoveTropics.location("entity/chest/trivia"));
	private static final Material GLOW_MATERIAL = new Material(Sheets.CHEST_SHEET, LoveTropics.location("entity/chest/trivia_glow"));
	private static final Material INACTIVE_MATERIAL = new Material(Sheets.CHEST_SHEET, LoveTropics.location("entity/chest/trivia_inactive"));

	private final ModelPart lid;
	private final ModelPart bottom;
	private final ModelPart lock;

	public TriviaChestRenderer(BlockEntityRendererProvider.Context context) {
		ModelPart root = context.bakeLayer(ModelLayers.CHEST);
		bottom = root.getChild("bottom");
		lid = root.getChild("lid");
		lock = root.getChild("lock");
	}

	@Override
	public void render(TriviaChestBlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, Vec3 cameraPos) {
		BlockState blockState = blockEntity.getBlockState();

		poseStack.pushPose();
		float rotation = blockEntity.hasLevel() ? blockState.getOptionalValue(ChestBlock.FACING).orElse(Direction.NORTH).toYRot() : Direction.SOUTH.toYRot();
		poseStack.translate(0.5f, 0.5f, 0.5f);
		poseStack.mulPose(Axis.YP.rotationDegrees(-rotation));
		poseStack.translate(-0.5f, -0.5f, -0.5f);

		float openness = blockEntity.getOpenNess(partialTicks);
		openness = 1.0f - openness;
		openness = 1.0f - openness * openness * openness;

		if (!blockState.getValue(TriviaChestBlock.ANSWERED)) {
			render(poseStack, MATERIAL.buffer(bufferSource, RenderType::entityCutout), lid, lock, bottom, openness, packedLight, packedOverlay);
			render(poseStack, GLOW_MATERIAL.buffer(bufferSource, RenderType::entityCutout), lid, lock, bottom, openness, LightTexture.FULL_BRIGHT, packedOverlay);
		} else {
			render(poseStack, INACTIVE_MATERIAL.buffer(bufferSource, RenderType::entityCutout), lid, lock, bottom, openness, packedLight, packedOverlay);
		}

		poseStack.popPose();
	}

	private void render(PoseStack poseStack, VertexConsumer consumer, ModelPart lid, ModelPart lock, ModelPart bottom, float lidAngle, int packedLight, int packedOverlay) {
		lid.xRot = -lidAngle * Mth.HALF_PI;
		lock.xRot = lid.xRot;
		lid.render(poseStack, consumer, packedLight, packedOverlay);
		lock.render(poseStack, consumer, packedLight, packedOverlay);
		bottom.render(poseStack, consumer, packedLight, packedOverlay);
	}
}
