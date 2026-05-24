package com.lovetropics.minigames.client.render.entity;

import com.lovetropics.minigames.client.render.entity.state.PlatformRenderState;
import com.lovetropics.minigames.common.content.survive_the_tide.entity.PlatformEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import org.joml.Matrix4f;

public final class PlatformRenderer extends EntityRenderer<PlatformEntity, PlatformRenderState> {

	private final ModelManager modelManager;

	public PlatformRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.modelManager = Minecraft.getInstance().getModelManager();
	}

	@Override
	public PlatformRenderState createRenderState() {
		return new PlatformRenderState();
	}

	@Override
	public void extractRenderState(PlatformEntity entity, PlatformRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.width = entity.getWidth();
		state.sprite = modelManager.getBlockStateModelSet().getParticleMaterial(entity.getBlockState()).sprite();
	}

	@Override
	public void submit(PlatformRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		super.submit(state, poseStack, submitNodeCollector, camera);

		TextureAtlasSprite sprite = state.sprite;
		if (sprite == null) {
			return;
		}

		// This is an extreme hack, and not guaranteed to be correct
		Identifier texture = sprite.contents().name().withPath(p -> "textures/" + p + ".png");

		float width = state.width;
		submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entitySolid(texture), ((pose, consumer) -> {
			consumer.addVertex(pose, -width / 2.0f, 0.0f, -width / 2.0f).setColor(CommonColors.WHITE).setUv(0.0f, 0.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(state.lightCoords).setNormal(0.0f, 1.0f, 0.0f);
			consumer.addVertex(pose, -width / 2.0f, 0.0f, width / 2.0f).setColor(CommonColors.WHITE).setUv(0.0f, width).setOverlay(OverlayTexture.NO_OVERLAY).setLight(state.lightCoords).setNormal(0.0f, 1.0f, 0.0f);
			consumer.addVertex(pose, width / 2.0f, 0.0f, width / 2.0f).setColor(CommonColors.WHITE).setUv(width, width).setOverlay(OverlayTexture.NO_OVERLAY).setLight(state.lightCoords).setNormal(0.0f, 1.0f, 0.0f);
			consumer.addVertex(pose, width / 2.0f, 0.0f, -width / 2.0f).setColor(CommonColors.WHITE).setUv(width, 0.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(state.lightCoords).setNormal(0.0f, 1.0f, 0.0f);
		}));

	}
}
