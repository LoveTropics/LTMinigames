package com.lovetropics.minigames.common.content.escape_race.misc;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.game.ClientGameStateManager;
import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import com.lovetropics.minigames.common.content.escape_race.client.EscapeRaceClientBucksState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Quaternionf;

public class RoomEntrancePadEntityRenderer extends EntityRenderer<RoomEntrancePadEntity, RoomEntrancePadRenderState> {
	private static final Identifier TEXTURE = LoveTropics.id("textures/entity/room_entrance_pad.png");
	private static final int LOCKED_COLOR = 0xFFFF0000;
	private static final int UNLOCKED_COLOR = 0xFF0000FF;

	private final ItemModelResolver itemModelResolver;
	private final Font font;
	private final EntityRenderDispatcher entityRenderDispatcher;

	public RoomEntrancePadEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
		itemModelResolver = context.getItemModelResolver();
		font = context.getFont();
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
		reusedState.height = Math.min(entity.getHeight(), 1.0f);
		reusedState.width = entity.getWidth();
		reusedState.ticks = entity.tickCount;
		reusedState.cost = entity.getCost();
		reusedState.roomName = entity.getRoomName();
		float unlockProgress = entity.getUnlockProgress(partialTick);
		reusedState.color = ARGB.linearLerp(unlockProgress, LOCKED_COLOR, UNLOCKED_COLOR);
		reusedState.height = Mth.lerp(unlockProgress, reusedState.height, entity.getHeight());
		EscapeRaceClientBucksState breakBuckState = ClientGameStateManager.getOrNull(EscapeRace.BREAK_BUCK_STATE);
		reusedState.canAfford = breakBuckState == null || breakBuckState.amount() >= entity.getCost();
		itemModelResolver.updateForNonLiving(reusedState.breakBuck, EscapeRace.BREAK_BUCK.asStack(1), ItemDisplayContext.FIXED, entity);
	}

	@Override
	public void submit(RoomEntrancePadRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		super.submit(renderState, poseStack, submitNodeCollector, camera);

		poseStack.pushPose();
		poseStack.mulPose(Axis.ZP.rotationDegrees(180f)); // Turn upsidedown
		float minX = -(renderState.width / 2);
		float minY = -renderState.height;
		float minZ = -(renderState.depth / 2);
		float maxX = renderState.width / 2;
		float maxY = 0.0f;
		float maxZ = renderState.depth / 2;
		int packedLight = renderState.lightCoords;
		submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(TEXTURE, false), (pose, buffer) -> {
			renderFace(packedLight, buffer, pose, renderState.color, minX, minY, minZ, maxX, maxY, minZ, renderState.ticks);
			renderFace(packedLight, buffer, pose, renderState.color, minX, minY, minZ, minX, maxY, maxZ, renderState.ticks);
			renderFace(packedLight, buffer, pose, renderState.color, maxX, minY, maxZ, minX, maxY, maxZ, renderState.ticks);
			renderFace(packedLight, buffer, pose, renderState.color, maxX, minY, maxZ, maxX, maxY, minZ, renderState.ticks);
		});

		poseStack.popPose();

		poseStack.pushPose();
		int backgroundColor = ARGB.color(Minecraft.getInstance().options.getBackgroundOpacity(0.25F), CommonColors.BLACK);
		poseStack.translate(0.0f, 0.5f, 0.0f);

		float textScale = 0.05f;
		String costText = renderState.cost + "x";
		int costTextWidth = font.width(costText);

		float buckWidth = 0.7f;
		float totalWidth = buckWidth + costTextWidth * textScale;

		poseStack.pushPose();
		poseStack.mulPose(Mth.rotationAroundAxis(Mth.Y_AXIS, entityRenderDispatcher.camera.rotation(), new Quaternionf()));
		poseStack.translate(-totalWidth / 2.0f + buckWidth / 2.0f, 0.0f, 0.0f);
		poseStack.pushPose();
		poseStack.scale(-1.0f, 1.0f, -1.0f);
		renderState.breakBuck.submit(poseStack, submitNodeCollector, packedLight, OverlayTexture.NO_OVERLAY, renderState.outlineColor);
		poseStack.popPose();

		int color = renderState.canAfford ? CommonColors.WHITE : CommonColors.SOFT_RED;
		poseStack.translate(buckWidth, 0.0f, 0.0f);
		poseStack.scale(textScale, -textScale, textScale);
		submitNodeCollector.submitText(poseStack,
				0,
				-font.lineHeight / 2.0f,
				FormattedCharSequence.forward(costText, Style.EMPTY),
				false,
				Font.DisplayMode.NORMAL,
				packedLight,
				color,
				backgroundColor,
				renderState.outlineColor
				);
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(renderState.yRot));
		poseStack.translate(0.0f, 2.5f, -renderState.depth / 2.0f);
		poseStack.scale(0.08f, -0.08f, 0.08f);
		submitNodeCollector.submitText(poseStack,
				-font.width(renderState.roomName) / 2.0f,
				0.0f,
				renderState.roomName.getVisualOrderText(),
				false,
				Font.DisplayMode.NORMAL,
				packedLight,
				CommonColors.WHITE,
				backgroundColor,
				renderState.outlineColor
		);
		poseStack.popPose();

		poseStack.popPose();
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
