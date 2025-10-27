package com.lovetropics.minigames.common.content.escape_race.ddr_machine;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import com.lovetropics.minigames.common.content.escape_race.vending_machine.VendingMachineEntity;
import com.lovetropics.minigames.common.content.escape_race.vending_machine.VendingMachineEntityModel;
import com.lovetropics.minigames.common.content.escape_race.vending_machine.VendingMachineRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

public class DDRMachineEntityRenderer extends EntityRenderer<DDRMachineEntity, DDRMachineRenderState> {
	private static final ResourceLocation TEXTURE = LoveTropics.location("textures/entity/ddr_machine.png");

	private final DDRMachineEntityModel model;

	public DDRMachineEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
		model = new DDRMachineEntityModel(context.bakeLayer(DDRMachineEntityModel.LAYER_LOCATION));
	}

	@Override
	public DDRMachineRenderState createRenderState() {
		return new DDRMachineRenderState();
	}

	@Override
	public void extractRenderState(DDRMachineEntity entity, DDRMachineRenderState reusedState, float partialTick) {
		super.extractRenderState(entity, reusedState, partialTick);
		reusedState.yRot = entity.getYRot();
		reusedState.foldAnimationState.copyFrom(entity.foldIntoBedState);
	}

	@Override
	public void render(DDRMachineRenderState renderState, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		super.render(renderState, poseStack, bufferSource, packedLight);
		poseStack.pushPose();
		poseStack.translate(0.0, 1.5, 0.0); // Roughly get into the center of the place
		this.model.setupAnim(renderState);

		poseStack.mulPose(Axis.YP.rotationDegrees(90 - renderState.yRot)); // Facing
		poseStack.pushPose();
		poseStack.mulPose(Axis.ZP.rotationDegrees(180f)); // Turn upsidedown

		VertexConsumer builder = bufferSource.getBuffer(model.renderType(TEXTURE));
		model.renderToBuffer(poseStack, builder, packedLight, OverlayTexture.NO_OVERLAY); // Render the model
		poseStack.popPose();
		poseStack.popPose();
	}
}
