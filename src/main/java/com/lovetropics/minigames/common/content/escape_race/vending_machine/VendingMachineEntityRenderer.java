package com.lovetropics.minigames.common.content.escape_race.vending_machine;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.render.entity.DriftwoodRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

public class VendingMachineEntityRenderer extends EntityRenderer<VendingMachineEntity, VendingMachineRenderState> {
	private static final ResourceLocation TEXTURE = LoveTropics.location("textures/entity/vending_machine.png");

	private final VendingMachineEntityModel model;

	public VendingMachineEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
		model = new VendingMachineEntityModel(context.bakeLayer(VendingMachineEntityModel.LAYER_LOCATION));
	}

	@Override
	public VendingMachineRenderState createRenderState() {
		return new VendingMachineRenderState();
	}

	@Override
	public void extractRenderState(VendingMachineEntity entity, VendingMachineRenderState reusedState, float partialTick) {
		super.extractRenderState(entity, reusedState, partialTick);
		reusedState.yRot = entity.getYRot();
	}

	@Override
	public void render(VendingMachineRenderState renderState, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		super.render(renderState, poseStack, bufferSource, packedLight);
		poseStack.pushPose();
		poseStack.translate(0.0, 1.5, 0.0);
		poseStack.mulPose(Axis.YP.rotationDegrees(180 - renderState.yRot));
		poseStack.mulPose(Axis.ZP.rotationDegrees(180f));

		VertexConsumer builder = bufferSource.getBuffer(model.renderType(TEXTURE));
		model.renderToBuffer(poseStack, builder, packedLight, OverlayTexture.NO_OVERLAY);

		poseStack.popPose();
	}
}
