package com.lovetropics.minigames.common.content.escape_race.misc;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.game.ClientGameStateManager;
import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import com.lovetropics.minigames.common.content.escape_race.client.EscapeRaceClientBucksState;
import com.lovetropics.minigames.common.content.escape_race.client.EscapeRaceRoomsState;
import com.lovetropics.minigames.common.content.escape_race.rooms.RoomStatus;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiSpriteManager;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

import java.util.Objects;

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
		EscapeRaceRoomsState roomsState = Objects.requireNonNullElse(ClientGameStateManager.getOrNull(EscapeRace.ROOMS_STATE), EscapeRaceRoomsState.EMPTY);
		EscapeRaceRoomsState.Room room = Objects.requireNonNullElse(roomsState.byEntrance(entity), EscapeRaceRoomsState.Room.EMPTY);
		reusedState.roomStatus = room.status();
		reusedState.cost = room.cost();
		reusedState.roomName = room.name();
		reusedState.color = switch(room.status()){
			case LOCKED -> 0xFFFF0000;
			case UNLOCKED -> 0xFF0000FF;
			case COMPLETED -> 0xFF00FF00;
		};
		EscapeRaceClientBucksState breakBuckState = ClientGameStateManager.getOrNull(EscapeRace.BREAK_BUCK_STATE);
		int breakBucks = breakBuckState != null ? breakBuckState.amount() : 0;
		reusedState.canAfford = breakBucks >= room.cost();
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

		int backgroundColor = ARGB.color(Minecraft.getInstance().options.getBackgroundOpacity(0.25F), CommonColors.BLACK);
		poseStack.mulPose(Axis.YP.rotationDegrees(180)); // Facing
		poseStack.translate(0.0f, 0.5f, 0.0f);
		poseStack.mulPose(Mth.rotationAroundAxis(Mth.Y_AXIS, entityRenderDispatcher.camera.rotation(), new Quaternionf()));

		if (renderState.roomStatus == RoomStatus.LOCKED) {
			float textScale = 0.05f;
			String costText = renderState.cost + "x";
			int costTextWidth = font.width(costText);

			float buckWidth = 0.7f;
			float totalWidth = buckWidth + costTextWidth * textScale;

			poseStack.pushPose();
			poseStack.translate(-totalWidth / 2.0f + buckWidth / 2.0f, 0.0f, 0.0f);
			renderState.breakBuck.render(poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);

			int color = renderState.canAfford ? CommonColors.WHITE : CommonColors.SOFT_RED;
			poseStack.translate(buckWidth, 0.0f, 0.0f);
			poseStack.scale(textScale, -textScale, textScale);
			font.drawInBatch(
					costText,
					0.0f,
					-font.lineHeight / 2.0f,
					color, false, poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, backgroundColor, packedLight
			);
			poseStack.popPose();
		}

		poseStack.pushPose();
		poseStack.translate(0.0f, 1.5, 0.0f);
		poseStack.scale(0.08f, -0.08f, 0.08f);
		font.drawInBatch(
				renderState.roomName,
				-font.width(renderState.roomName) / 2.0f,
				0.0f,
				CommonColors.WHITE, false, poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, backgroundColor, packedLight
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
