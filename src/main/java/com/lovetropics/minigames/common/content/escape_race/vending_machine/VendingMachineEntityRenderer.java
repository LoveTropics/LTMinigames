package com.lovetropics.minigames.common.content.escape_race.vending_machine;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
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

public class VendingMachineEntityRenderer extends EntityRenderer<VendingMachineEntity, VendingMachineRenderState> {
	private static final ResourceLocation TEXTURE = LoveTropics.location("textures/entity/vending_machine.png");

	private final VendingMachineEntityModel model;
	private final ItemModelResolver itemModelResolver;

	public VendingMachineEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
		model = new VendingMachineEntityModel(context.bakeLayer(VendingMachineEntityModel.LAYER_LOCATION));
		this.itemModelResolver = context.getItemModelResolver();
	}

	@Override
	public VendingMachineRenderState createRenderState() {
		return new VendingMachineRenderState();
	}

	@Override
	public void extractRenderState(VendingMachineEntity entity, VendingMachineRenderState reusedState, float partialTick) {
		super.extractRenderState(entity, reusedState, partialTick);
		reusedState.yRot = entity.getYRot();
		for (int i = 0; i < Objects.requireNonNull(reusedState.items).size(); i++) {
			ItemStackRenderState stack = reusedState.items.get(i);
			if(entity.getItems().size() >= i){
				ItemStack itemStack = entity.getItems().get(i);
				itemModelResolver.updateForNonLiving(stack, itemStack, ItemDisplayContext.FIXED, entity);
			}
		}
		reusedState.isLookingAt = Minecraft.getInstance().crosshairPickEntity == entity;
		reusedState.selectedIndex = entity.getSelected();
		if(reusedState.selectedIndex != -1) {
			ItemStack itemStack = entity.getItems().get(reusedState.selectedIndex);
			if(itemStack.has(EscapeRace.VENDINGMACHINE_COMPONENT)){
				reusedState.selectedCost = itemStack.get(EscapeRace.VENDINGMACHINE_COMPONENT);
			}
			reusedState.selectedName = itemStack.getHoverName().getString();
		} else {
			reusedState.selectedCost = -1;
		}
		if(entity.getDroppingItem() != null){
			itemModelResolver.updateForNonLiving(reusedState.droppingItem, entity.getDroppingItem(), ItemDisplayContext.FIXED, entity);
		}
		reusedState.droppingItemProgress = entity.getDroppingItemProgress();
		reusedState.droppingItemStart = entity.getDroppingItemStart();
	}

	@Override
	public void render(VendingMachineRenderState renderState, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		super.render(renderState, poseStack, bufferSource, packedLight);
		poseStack.pushPose();
		poseStack.translate(0.0, 1.5, 0.0); // Roughly get into the center of the place

		poseStack.mulPose(Axis.YP.rotationDegrees(180 - renderState.yRot)); // Facing
		poseStack.pushPose();
		poseStack.mulPose(Axis.ZP.rotationDegrees(180f)); // Turn upsidedown

		VertexConsumer builder = bufferSource.getBuffer(model.renderType(TEXTURE));
		model.renderToBuffer(poseStack, builder, packedLight, OverlayTexture.NO_OVERLAY); // Render the model
		poseStack.popPose();
		int i = 0;
		for (int renderStateIndex = 0; renderStateIndex < renderState.items.size(); renderStateIndex++) {
			ItemStackRenderState item = renderState.items.get(renderStateIndex);
			if(item.isEmpty())
				continue;
			if(renderStateIndex >= VendingMachineEntity.SLOTS.size()){
				continue;
			}
			VendingMachineEntity.VendingMachineSlot slot = VendingMachineEntity.SLOTS.get(renderStateIndex);
			var size = item.getModelBoundingBox().getSize();
			float scale = 0.4f;
			if(size > 0.5){
				float diffInSize = (float) size - 0.5f; // Account for different item model scales (ish)
				scale = scale - diffInSize;
			}
			poseStack.pushPose();
			poseStack.translate(slot.x(), slot.y(), slot.z());
			poseStack.scale(scale, scale, scale);
			for(int x = 0; x < 4; x++){
				poseStack.pushPose();
				poseStack.translate(0, 0, 0.5*x);
				// Invert the posestack last matrix
				// subtract camera position and Apply to hitresult
				boolean isHighlighted = false;
				if(x == 0) {
					if (renderState.isLookingAt) {
						if(renderStateIndex == renderState.selectedIndex){
							isHighlighted = true;
						}
					}
				}
				if(isHighlighted) {
					poseStack.pushPose();
					poseStack.scale(1.25f, 1.25f, 1.25f);
					item.render(poseStack, Minecraft.getInstance().renderBuffers().outlineBufferSource(), packedLight, OverlayTexture.NO_OVERLAY);
					String name = renderState.selectedName;
					Font font = Minecraft.getInstance().font;
					float xOffset = -font.width(name) / 2f;
					int j = (int)(Minecraft.getInstance().options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
					poseStack.popPose();
					poseStack.pushPose();
					poseStack.mulPose(Axis.ZP.rotationDegrees(-180f)); // Turn upsidedown
					poseStack.scale(0.03f, 0.03f, 0.03f);
					font.drawInBatch(
							name,
							xOffset,
							-30f,
							-2130706433, false, poseStack.last().pose(), bufferSource, Font.DisplayMode.SEE_THROUGH, j, packedLight
					);
					poseStack.popPose();
				} else {
					item.render(poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
				}
				poseStack.popPose();
			}
			poseStack.popPose();
			i++;
		}
		if(!renderState.droppingItem.isEmpty()){
			ItemStackRenderState item = renderState.droppingItem;
			var size = item.getModelBoundingBox().getSize();
			float scale = 0.4f;
			if(size > 0.5){
				float diffInSize = (float) size - 0.5f; // Account for different item model scales (ish)
				scale = scale - diffInSize;
			}
			poseStack.pushPose();
			net.minecraft.world.phys.Vec3 pos = Mth.lerp(renderState.droppingItemProgress, new Vec3(renderState.droppingItemStart), new Vec3(renderState.droppingItemStart.x, -1, renderState.droppingItemStart.z ));
			float yRot = Mth.lerp(renderState.droppingItemProgress, 0, 360);
			poseStack.translate(pos);
			poseStack.scale(scale, scale, scale);
			poseStack.mulPose(Axis.XN.rotationDegrees(yRot));
			item.render(poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
			poseStack.popPose();
		}
		poseStack.popPose();
	}
}
