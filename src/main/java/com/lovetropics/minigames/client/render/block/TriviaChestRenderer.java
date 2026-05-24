package com.lovetropics.minigames.client.render.block;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.render.block.state.TriviaChestBlockEntityRenderState;
import com.lovetropics.minigames.common.content.river_race.block.TriviaChestBlock;
import com.lovetropics.minigames.common.content.river_race.block.TriviaChestBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class TriviaChestRenderer implements BlockEntityRenderer<TriviaChestBlockEntity, TriviaChestBlockEntityRenderState> {
	private static final SpriteId MATERIAL = new SpriteId(Sheets.CHEST_SHEET, LoveTropics.id("entity/chest/trivia"));
	private static final SpriteId GLOW_MATERIAL = new SpriteId(Sheets.CHEST_SHEET, LoveTropics.id("entity/chest/trivia_glow"));
	private static final SpriteId INACTIVE_MATERIAL = new SpriteId(Sheets.CHEST_SHEET, LoveTropics.id("entity/chest/trivia_inactive"));

	private final ModelPart lid;
	private final ModelPart bottom;
	private final ModelPart lock;
	private final SpriteGetter sprites;

	public TriviaChestRenderer(BlockEntityRendererProvider.Context context) {
		ModelPart root = context.bakeLayer(ModelLayers.CHEST);
		bottom = root.getChild("bottom");
		lid = root.getChild("lid");
		lock = root.getChild("lock");
		this.sprites = context.sprites();
	}

	@Override
	public void submit(TriviaChestBlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {

		poseStack.pushPose();
		float rotation = state.rotation;
		poseStack.translate(0.5f, 0.5f, 0.5f);
		poseStack.mulPose(Axis.YP.rotationDegrees(-rotation));
		poseStack.translate(-0.5f, -0.5f, -0.5f);

		float openness = state.openness;
		openness = 1.0f - openness;
		openness = 1.0f - openness * openness * openness;
		float finalOpenness = openness;
		if (!state.answered) {
			lid.xRot = -finalOpenness * Mth.HALF_PI;
			lock.xRot = lid.xRot;
			submitPart(submitNodeCollector, MATERIAL, poseStack, state.lightCoords);
			submitPart(submitNodeCollector, GLOW_MATERIAL, poseStack, LightCoordsUtil.FULL_BRIGHT);
		} else {
			submitPart(submitNodeCollector, MATERIAL, poseStack, state.lightCoords);
		}

		poseStack.popPose();
	}

	private void submitPart(SubmitNodeCollector collector, SpriteId id, PoseStack poseStack, int lightCords) {
		RenderType renderType = id.renderType(RenderTypes::entityCutout);
		TextureAtlasSprite sprite = this.sprites.get(id);
		collector.submitModelPart(lid, poseStack, renderType, lightCords, OverlayTexture.NO_OVERLAY, sprite);
		collector.submitModelPart(bottom, poseStack, renderType, lightCords, OverlayTexture.NO_OVERLAY, sprite);
		collector.submitModelPart(lock, poseStack, renderType, lightCords, OverlayTexture.NO_OVERLAY, sprite);
	}

	@Override
	public TriviaChestBlockEntityRenderState createRenderState() {
		return new TriviaChestBlockEntityRenderState();
	}

	@Override
	public void extractRenderState(TriviaChestBlockEntity blockEntity, TriviaChestBlockEntityRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockState blockState = blockEntity.getBlockState();

		state.rotation = blockEntity.hasLevel() ? blockState.getOptionalValue(ChestBlock.FACING).orElse(Direction.NORTH).toYRot() : Direction.SOUTH.toYRot();
		state.openness = blockEntity.getOpenNess(partialTicks);
		state.answered = blockState.getValue(TriviaChestBlock.ANSWERED);
	}
}
