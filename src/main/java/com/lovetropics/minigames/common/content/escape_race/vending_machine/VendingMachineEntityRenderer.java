package com.lovetropics.minigames.common.content.escape_race.vending_machine;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.render.entity.DriftwoodRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CommonColors;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Objects;
import java.util.Optional;

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
	}

	@Override
	public void render(VendingMachineRenderState renderState, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		super.render(renderState, poseStack, bufferSource, packedLight);
//		if(renderState.isLookingAt){
//			Vec3 location = Minecraft.getInstance().hitResult.getLocation();
//			poseStack.pushPose();
//			poseStack.translate(location.subtract(renderState.x, renderState.y, renderState.z));
//			DebugRenderer.renderFilledBox(poseStack, bufferSource, 0, 0, 0, 0.1, 0.1, 0.1, 1, 1, 1, 1);
////			model.renderToBuffer(poseStack, builder, packedLight, OverlayTexture.NO_OVERLAY);
//			poseStack.popPose();
//		}
//		DebugRenderer.renderVoxelShape(poseStack, bufferSource.getBuffer(RenderType.lines()), Shapes.box(-0.1,-0.1,-0.1,0.1,0.1,0.1), 0, 0,0, 1F, 1F, 1F, 1F, true);
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
//						Matrix4f inverted = new Matrix4f();
//						poseStack.last().pose().invert(inverted);
//						Vector3f lookVector = Minecraft.getInstance().gameRenderer.getMainCamera().getLookVector();
//						Vec3 relativeWorldSpace = Vec3.ZERO;
//						Vector3f target = inverted.transformPosition(relativeWorldSpace.add(new Vec3(lookVector).scale(1.5f)).toVector3f(), new Vector3f());
//						Vector3f origin = inverted.transformPosition(relativeWorldSpace.toVector3f(), new Vector3f());
//						Optional<Vec3> clip = new AABB(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5).clip(new Vec3(origin), new Vec3(target));
//						if (clip.isPresent()) {
//							isHighlighted = true;
//						}
					}
				}
				if(isHighlighted) {
					poseStack.pushPose();
					poseStack.scale(1.25f, 1.25f, 1.25f);
					item.render(poseStack, Minecraft.getInstance().renderBuffers().outlineBufferSource(), packedLight, OverlayTexture.NO_OVERLAY);
					String name = "Item Name";
					Font font = Minecraft.getInstance().font;
					float xOffset = -font.width(name) / 2f;
					int j = (int)(Minecraft.getInstance().options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
					poseStack.popPose();
					poseStack.pushPose();
					poseStack.mulPose(Axis.ZP.rotationDegrees(-180f)); // Turn upsidedown
					poseStack.scale(0.05f, 0.05f, 0.05f);
					font.drawInBatch(
							"Item Name",
							xOffset,
							-20f,
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
		poseStack.popPose();
	}
}
