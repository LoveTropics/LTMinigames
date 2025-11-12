package com.lovetropics.minigames.common.content.escape_race.misc;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import com.lovetropics.minigames.common.content.escape_race.rooms.RoomStatus;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiSpriteManager;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

public class RoomEntrancePadEntityRenderer extends EntityRenderer<RoomEntrancePadEntity, RoomEntrancePadRenderState> {
	private static final ResourceLocation TEXTURE = LoveTropics.location("textures/entity/room_entrance_pad.png");
	private final GuiSpriteManager guiSpriteManager;
	private final ItemModelResolver itemModelResolver;
	private final Font font;
	private final EntityRenderDispatcher entityRenderDispatcher;

	private final ItemStack breakBuck;

	public RoomEntrancePadEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
		guiSpriteManager = Minecraft.getInstance().getGuiSprites();
		itemModelResolver = context.getItemModelResolver();
		font = context.getFont();
		breakBuck = EscapeRace.BREAK_BUCK.asStack(1);
		this.entityRenderDispatcher = context.getEntityRenderDispatcher();
	}

	@Override
	public RoomEntrancePadRenderState createRenderState() {
		return new RoomEntrancePadRenderState();
	}

	@Override
	public void extractRenderState(RoomEntrancePadEntity entity, RoomEntrancePadRenderState reusedState, float partialTick) {
		super.extractRenderState(entity, reusedState, partialTick);
		reusedState.yRot = entity.getYRot();
		reusedState.depth = entity.getDepth();
		reusedState.height = entity.getHeight();
		reusedState.width = entity.getWidth();
		reusedState.ticks = entity.tickCount;
		reusedState.roomStatus = entity.getRoomStatus();
		reusedState.cost = entity.getCost();
		reusedState.roomName = entity.getRoomName();
		itemModelResolver.updateForNonLiving(reusedState.breakBuck, breakBuck, ItemDisplayContext.FIXED, entity);
	}

	@Override
	public void render(RoomEntrancePadRenderState renderState, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		super.render(renderState, poseStack, bufferSource, packedLight);
		poseStack.pushPose();
		poseStack.translate(0.0, 0, 0.0); // Roughly get into the center of the place
		poseStack.mulPose(Axis.YP.rotationDegrees(renderState.yRot)); // Facing
		poseStack.pushPose();
		poseStack.mulPose(Axis.ZP.rotationDegrees(180f)); // Turn upsidedown
		float minX = -(renderState.width / 2);
		float minY = -(renderState.height / 2);
		float minZ = -(renderState.depth / 2);
		float maxX = renderState.width / 2;
		float maxY = renderState.height / 2;
		float maxZ = renderState.depth / 2;
		VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE, false));
		PoseStack.Pose last = poseStack.last();
		renderFace(packedLight, buffer, last, renderState.color, minX, minY, minZ, maxX, maxY, minZ, renderState.ticks);
		renderFace(packedLight, buffer, last, renderState.color, minX, minY, minZ, minX, maxY, maxZ, renderState.ticks);
		renderFace(packedLight, buffer, last, renderState.color, maxX, minY, maxZ, minX, maxY, maxZ, renderState.ticks);
		renderFace(packedLight, buffer, last, renderState.color, maxX, minY, maxZ, maxX, maxY, minZ, renderState.ticks);
		poseStack.popPose();
		if(renderState.roomStatus == RoomStatus.LOCKED){
			poseStack.mulPose(Axis.YP.rotationDegrees(180)); // Facing
			poseStack.mulPose(this.calculateOrientation(new Quaternionf()));
			poseStack.translate(-0.4f,  0.5, 0f);
			poseStack.pushPose();
			poseStack.translate(-0.1f,  0.1f, 0f);
			renderState.breakBuck.render(poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
			poseStack.popPose();
			String text = renderState.cost + "x";
			float xOffset = -font.width(text) / 2f;
			int j = (int)(Minecraft.getInstance().options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
			poseStack.pushPose();
			poseStack.translate(0.8f,  .3, 0f);
			poseStack.mulPose(Axis.ZP.rotationDegrees(-180f)); // Turn upsidedown
			poseStack.scale(0.05f, 0.05f, 0.05f);
			font.drawInBatch(
					text,
					xOffset - 2f,
					0f,
					-1, false, poseStack.last().pose(), bufferSource, Font.DisplayMode.SEE_THROUGH, j, packedLight
			);
			poseStack.popPose();
			poseStack.pushPose();
			poseStack.translate(0.4f,  1.5, 0f);
			poseStack.mulPose(Axis.ZP.rotationDegrees(-180f)); // Turn upsidedown
			poseStack.scale(0.08f, 0.08f, 0.08f);
			float nameXOffset = -font.width(renderState.roomName) / 2f;
			font.drawInBatch(
					renderState.roomName,
					nameXOffset,
					0f,
					-1, false, poseStack.last().pose(), bufferSource, Font.DisplayMode.SEE_THROUGH, j, packedLight
			);
			poseStack.popPose();
		}
		poseStack.popPose();
	}

	private Quaternionf calculateOrientation(Quaternionf quaternion) {
		Camera camera = this.entityRenderDispatcher.camera;
		return quaternion.rotationYXZ((float) (-Math.PI / 180.0) * cameraYrot(camera), (float) (-Math.PI / 180.0) * cameraXRot(camera), 0.0F);
	}

	private static float cameraYrot(Camera camera) {
		return camera.getYRot() - 180.0F;
	}

	private static float cameraXRot(Camera camera) {
		return -camera.getXRot();
	}

	private static void renderFace(int packedLight, VertexConsumer buffer, PoseStack.Pose last, int color, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int ticks) {
		int currentFrame = ticks - ((ticks / 8) * 8);
		buffer
				.addVertex(last.pose(), minX, maxY, minZ)
				.setUv(0, 0.125f * (currentFrame + 1))
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(packedLight)
				.setColor(color)
				.setNormal(0, 0,0);
		buffer
				.addVertex(last.pose(), maxX, maxY, maxZ)
				.setUv(1, 0.125f  * (currentFrame + 1))
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(packedLight)
				.setColor(color)
				.setNormal(0, 0,0);
		buffer
				.addVertex(last.pose(), maxX, minY, maxZ)
				.setUv(1, (0.125f  * currentFrame))
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(packedLight)
				.setColor(color)
				.setNormal(0, 0,0);
		buffer
				.addVertex(last.pose(), minX, minY, minZ)
				.setUv(0, (0.125f  * currentFrame))
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(packedLight)
				.setColor(color)
				.setNormal(0, 0,0);
	}
}
