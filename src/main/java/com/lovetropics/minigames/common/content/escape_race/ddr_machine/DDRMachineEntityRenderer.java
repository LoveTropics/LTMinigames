package com.lovetropics.minigames.common.content.escape_race.ddr_machine;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.DDRMachineLevelClient;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.DDRMachineLevelClientRenderState;
import com.lovetropics.minigames.common.content.escape_race.vending_machine.VendingMachineEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiSpriteManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DDRMachineEntityRenderer extends EntityRenderer<DDRMachineEntity, DDRMachineRenderState> {
	private static final ResourceLocation TEXTURE = LoveTropics.location("textures/entity/ddr_machine.png");
	private final DDRMachineEntityModel model;
	private final GuiSpriteManager guiSpriteManager;
	private final ItemModelResolver itemModelResolver;
	private final Font font;

	public DDRMachineEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
		model = new DDRMachineEntityModel(context.bakeLayer(DDRMachineEntityModel.LAYER_LOCATION));
		guiSpriteManager = Minecraft.getInstance().getGuiSprites();
		itemModelResolver = context.getItemModelResolver();
		font = context.getFont();
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
		reusedState.ddrMachineState = entity.getState();
		reusedState.input = entity.getPlayerInput();
		reusedState.upcomingMoves.clear();
		reusedState.upcomingMoves.putAll(entity.getUpcomingMoves());
		reusedState.currentTick = entity.getCurrentTick();
		reusedState.isRiding =  entity.getControllingPassenger() instanceof LocalPlayer;
		for (int i = 0; i < entity.getAvailableLevels().size(); i++) {
			if(reusedState.levels.size() <= i){
				reusedState.levels.add(new DDRMachineLevelClientRenderState());
			}
			DDRMachineLevelClient ddrMachineLevelClient = entity.getAvailableLevels().get(i);
			if(ddrMachineLevelClient.icon().isEmpty()){
				continue;
			}
			DDRMachineLevelClientRenderState ddrMachineLevelClientRenderState = reusedState.levels.get(i);
			ItemStackRenderState itemStackRenderState = ddrMachineLevelClientRenderState.itemStackRenderState;
			itemModelResolver.updateForNonLiving(itemStackRenderState, ddrMachineLevelClient.icon(), ItemDisplayContext.FIXED, entity);
			ddrMachineLevelClientRenderState.displayName = ddrMachineLevelClient.displayName();
		}
	}

	@Override
	public void render(DDRMachineRenderState renderState, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		super.render(renderState, poseStack, bufferSource, packedLight);
		Vector3f lookVector = Minecraft.getInstance().gameRenderer.getMainCamera().getLookVector();
		poseStack.pushPose();
		poseStack.translate(0.0, 1.5, 0.0); // Roughly get into the center of the place
		this.model.setupAnim(renderState);

		poseStack.mulPose(Axis.YP.rotationDegrees(90 - renderState.yRot)); // Facing
		poseStack.pushPose();
		poseStack.mulPose(Axis.ZP.rotationDegrees(180f)); // Turn upsidedown

		VertexConsumer builder = bufferSource.getBuffer(model.renderType(TEXTURE));
		model.renderToBuffer(poseStack, builder, packedLight, OverlayTexture.NO_OVERLAY);
		VertexConsumer buffer = bufferSource.getBuffer(RenderType.text(DDRMachineSprites.bgSprite.atlasLocation()));
		poseStack.popPose();
		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(-90F));
		poseStack.translate(-0.05, 0, 1.7f);
		poseStack.pushPose();
		poseStack.scale(1, 0.7f, 1f);
		drawTexture(buffer, poseStack, DDRMachineSprites.bgSprite, packedLight);
		poseStack.popPose();
		if(renderState.ddrMachineState == DDRMachineEntity.DDRMachineState.MENU) {
			poseStack.pushPose();
			poseStack.scale(0.5f, 0.2f, 0.5f);
			poseStack.translate(0, 2f,-0.1f);
			drawTexture(buffer, poseStack, DDRMachineSprites.logoSprite, packedLight);
			poseStack.popPose();
			poseStack.pushPose();
			poseStack.translate(0.8f, -0.05,0f);
			poseStack.scale(0.3f, 0.3f, 0.3f);
			var testSlots = List.of(
					new VendingMachineEntity.VendingMachineSlot(0f, 0.5f, 0f),
					new VendingMachineEntity.VendingMachineSlot(-1.5f, 0.5f, 0f),
					new VendingMachineEntity.VendingMachineSlot(1f, 0.5f, 0f),
					new VendingMachineEntity.VendingMachineSlot(1.5f, 0.5f, 0f),
					new VendingMachineEntity.VendingMachineSlot(0f, 0f, 0f),
					new VendingMachineEntity.VendingMachineSlot(0.5f, 0f, 0f),
					new VendingMachineEntity.VendingMachineSlot(1f, 0f, 0f),
					new VendingMachineEntity.VendingMachineSlot(1.5f, 0f, 0f)
			);
			int i = 0;
			for (DDRMachineLevelClientRenderState level : renderState.levels) {
				VendingMachineEntity.VendingMachineSlot slot = testSlots.get(i);
				poseStack.pushPose();
				poseStack.translate(slot.x(), slot.y(), slot.z());
				if(renderState.isRiding) {
					Matrix4f inverted = new Matrix4f();
					poseStack.last().pose().invert(inverted);
					Vec3 relativeWorldSpace = Vec3.ZERO;
					Vector3f target = inverted.transformPosition(relativeWorldSpace.add(new Vec3(lookVector).scale(1.5f)).toVector3f(), new Vector3f());
					Vector3f origin = inverted.transformPosition(relativeWorldSpace.toVector3f(), new Vector3f());
					Optional<Vec3> clip = new AABB(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5).clip(new Vec3(origin), new Vec3(target));
					if (clip.isPresent()) {
						poseStack.pushPose();
						poseStack.scale(1.25f, 1.25f, 1.25f);
						OutlineBufferSource bufferSource1 = Minecraft.getInstance().renderBuffers().outlineBufferSource();
						bufferSource1.setColor(255, 255, 255, 255);
						level.itemStackRenderState.render(poseStack, bufferSource1, packedLight, OverlayTexture.NO_OVERLAY);
						poseStack.popPose();
					} else {
						level.itemStackRenderState.render(poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
					}
				} else {
					level.itemStackRenderState.render(poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
				}
				float xOffset = -font.width(level.displayName) / 2f;
				int j = (int) (Minecraft.getInstance().options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
				poseStack.pushPose();
				poseStack.mulPose(Axis.ZP.rotationDegrees(-180f)); // Turn upsidedown
				poseStack.scale(0.03f, 0.03f, 0.03f);
				font.drawInBatch(
						level.displayName,
						xOffset,
						20f,
						-1, false, poseStack.last().pose(), bufferSource, Font.DisplayMode.SEE_THROUGH, j, packedLight
				);
				poseStack.popPose();
				poseStack.popPose();
				i++;
			}
			poseStack.popPose();
		} else if(renderState.ddrMachineState == DDRMachineEntity.DDRMachineState.PLAYING || renderState.ddrMachineState == DDRMachineEntity.DDRMachineState.RECORDING){
			poseStack.pushPose();
			poseStack.scale(0.2f, 0.2f, 0.2f);
			poseStack.translate(3.5, -2f,-0.1f);
			drawTexture(buffer, poseStack, renderState.input.left() ? DDRMachineSprites.leftFilledSprite : DDRMachineSprites.leftSprite, packedLight);
			poseStack.translate(-2.29f, 0, 0);
			drawTexture(buffer, poseStack, renderState.input.forward() ? DDRMachineSprites.upFilledSprite :DDRMachineSprites.upSprite, packedLight);
			poseStack.translate(-2.29f, 0, 0);
			drawTexture(buffer, poseStack, renderState.input.back() ? DDRMachineSprites.downFilledSprite :DDRMachineSprites.downSprite, packedLight);
			poseStack.translate(-2.29f, 0, 0);
			drawTexture(buffer, poseStack, renderState.input.right() ? DDRMachineSprites.rightFilledSprite :DDRMachineSprites.rightSprite, packedLight);
			poseStack.popPose();
			for (Map.Entry<Integer, DdrInput> entry : renderState.upcomingMoves.entrySet()) {
				int tick = entry.getKey();
				if(tick - renderState.currentTick > 20 * 4){
					continue;
				}
				DdrInput levelTick = entry.getValue();
				float yPos = Mth.lerp((tick - renderState.currentTick) / (20f * 4), -2f, 3f);
				if(levelTick.left()){
					poseStack.pushPose();
					poseStack.scale(0.2f, 0.2f, 0.2f);
					poseStack.translate(3.5, yPos,-0.1f);
					poseStack.scale(0.5f, 0.5f, 0.5f);
					drawTexture(buffer, poseStack, DDRMachineSprites.leftFilledSprite, packedLight);
					poseStack.popPose();
				}
				if(levelTick.right()){
					poseStack.pushPose();
					poseStack.scale(0.2f, 0.2f, 0.2f);
					poseStack.translate(-3.37, yPos,-0.1f);
					poseStack.scale(0.5f, 0.5f, 0.5f);
					drawTexture(buffer, poseStack, DDRMachineSprites.rightFilledSprite, packedLight);
					poseStack.popPose();
				}
				if(levelTick.forward()){
					poseStack.pushPose();
					poseStack.scale(0.2f, 0.2f, 0.2f);
					poseStack.translate(1.21, yPos,-0.1f);
					poseStack.scale(0.5f, 0.5f, 0.5f);
					drawTexture(buffer, poseStack, DDRMachineSprites.upFilledSprite, packedLight);
					poseStack.popPose();
				}
				if(levelTick.back()){
					poseStack.pushPose();
					poseStack.scale(0.2f, 0.2f, 0.2f);
					poseStack.translate(-1.08, yPos,-0.1f);
					poseStack.scale(0.5f, 0.5f, 0.5f);
					drawTexture(buffer, poseStack, DDRMachineSprites.downFilledSprite, packedLight);
					poseStack.popPose();
				}
			}
		}
		poseStack.popPose();
		poseStack.popPose();
	}

	public void drawTexture(VertexConsumer buffer, PoseStack poseStack, TextureAtlasSprite sprite, int packedLight) {
		Matrix4f matrix4f1 = poseStack.last().pose();
		buffer.addVertex(matrix4f1, -1F, 1.0F, -0.001F)
				.setColor(-1)
				.setUv(sprite.getU1(), sprite.getV0())
				.setLight(packedLight);
		buffer.addVertex(matrix4f1, 1F, 1.0F, -0.001F)
				.setColor(-1)
				.setUv(sprite.getU0(), sprite.getV0())
				.setLight(packedLight);
		buffer.addVertex(matrix4f1, 1F, -1.0F,-0.001F)
				.setColor(-1)
				.setUv(sprite.getU0(), sprite.getV1())
				.setLight(packedLight);
		buffer.addVertex(matrix4f1, -1F, -1.0F, -0.001F)
				.setColor(-1)
				.setUv(sprite.getU1(), sprite.getV1())
				.setLight(packedLight);
	}
}
