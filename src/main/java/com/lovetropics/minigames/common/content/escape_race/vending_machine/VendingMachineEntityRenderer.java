package com.lovetropics.minigames.common.content.escape_race.vending_machine;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
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
				reusedState.itemStacks.set(i, itemStack.copy());
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
						Matrix4f inverted = new Matrix4f();
						poseStack.last().pose().invert(inverted);
						Vector3f lookVector = Minecraft.getInstance().gameRenderer.getMainCamera().getLookVector();
						Vec3 relativeWorldSpace = Vec3.ZERO;
						Vector3f target = inverted.transformPosition(relativeWorldSpace.add(new Vec3(lookVector).scale(1.5f)).toVector3f(), new Vector3f());
						Vector3f origin = inverted.transformPosition(relativeWorldSpace.toVector3f(), new Vector3f());
						Optional<Vec3> clip = new AABB(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5).clip(new Vec3(origin), new Vec3(target));
						if (clip.isPresent()) {
							isHighlighted = true;
						}
					}
				}
				if(x == 0 && (isHighlighted || renderStateIndex == renderState.selectedIndex)) {
					poseStack.pushPose();
					poseStack.scale(1.25f, 1.25f, 1.25f);
					OutlineBufferSource bufferSource1 = Minecraft.getInstance().renderBuffers().outlineBufferSource();
					if(renderStateIndex == renderState.selectedIndex){
						bufferSource1.setColor(0,255, 0, 255);
					} else {
						bufferSource1.setColor(255, 255, 255, 255);
					}
					item.render(poseStack, bufferSource1, packedLight, OverlayTexture.NO_OVERLAY);
					String name = renderState.itemStacks.get(renderStateIndex).getHoverName().getString();
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

	public static void registerOverlays(RegisterGuiLayersEvent event) {
		event.registerAbove(VanillaGuiLayers.EXPERIENCE_LEVEL, LoveTropics.location("vending_machine_info"), (graphics, deltaTracker) -> {
			if (Minecraft.getInstance().options.hideGui) {
				return;
			}
			renderOverlay(graphics);
		});
	}

	private static void renderOverlay(GuiGraphics graphics) {
		if(Minecraft.getInstance().hitResult != null && Minecraft.getInstance().hitResult.getType() == HitResult.Type.ENTITY) {
			if(Minecraft.getInstance().hitResult instanceof EntityHitResult entityHitResult) {
				if(entityHitResult.getEntity() instanceof VendingMachineEntity entity) {
					if(entity.getLookAngle().dot(Minecraft.getInstance().player.getLookAngle()) < 1){
						int lookingAt = entity.calculatePlayerLookingAtSlot(Minecraft.getInstance().player);
						if(lookingAt != -1 && lookingAt < entity.getItems().size()){
							ItemStack lookingAtItem = entity.getItems().get(lookingAt);
							if(!lookingAtItem.isEmpty()) {
								Font font = Minecraft.getInstance().font;

								int width = font.width(lookingAtItem.getHoverName());
								int x = (graphics.guiWidth() / 2) + (-width / 2);
								int y = (graphics.guiHeight() - 80) + ((18 - font.lineHeight) / 2) + 8;

//								String currency = .getString();

								int i = ARGB.colorFromFloat(0.88f, 33 / 255f, 29/ 255f, 24/ 255f);
								if (i != 0) {
									int j = 2;
									graphics.fill(x - 2, y - 2, x + width + 2, y + 9 + 2, ARGB.multiply(i, -1));
								}
								graphics.drawStringWithBackdrop(
										font, lookingAtItem.getHoverName(),
										x,
										y,
										width,
										ARGB.color(1f, -1)
								);
							}
						}
					}
				}
			}
		}
	}
}
