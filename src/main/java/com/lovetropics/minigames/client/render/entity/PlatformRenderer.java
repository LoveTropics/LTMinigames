package com.lovetropics.minigames.client.render.entity;

import com.lovetropics.minigames.client.render.entity.state.PlatformRenderState;
import com.lovetropics.minigames.common.content.survive_the_tide.entity.PlatformEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CommonColors;
import org.joml.Matrix4f;

public final class PlatformRenderer extends EntityRenderer<PlatformEntity, PlatformRenderState> {
	private final BlockModelShaper blockModelShaper;

	public PlatformRenderer(EntityRendererProvider.Context context) {
		super(context);
		blockModelShaper = context.getBlockRenderDispatcher().getBlockModelShaper();
	}

	@Override
	public PlatformRenderState createRenderState() {
		return new PlatformRenderState();
	}

	@Override
	public void extractRenderState(PlatformEntity entity, PlatformRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.width = entity.getWidth();
		state.sprite = blockModelShaper.getParticleIcon(entity.getBlockState(), entity.level(), BlockPos.ZERO);
	}

	@Override
	public void render(PlatformRenderState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		super.render(state, poseStack, bufferSource, packedLight);

		TextureAtlasSprite sprite = state.sprite;
		if (sprite == null) {
			return;
		}

		// This is an extreme hack, and not guaranteed to be correct
		ResourceLocation texture = sprite.contents().name().withPath(p -> "textures/" + p + ".png");

		float width = state.width;
		VertexConsumer builder = bufferSource.getBuffer(RenderType.entitySolid(texture));
		Matrix4f pose = poseStack.last().pose();
		builder.addVertex(pose, -width / 2.0f, 0.0f, -width / 2.0f).setColor(CommonColors.WHITE).setUv(0.0f, 0.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0.0f, 1.0f, 0.0f);
		builder.addVertex(pose, -width / 2.0f, 0.0f, width / 2.0f).setColor(CommonColors.WHITE).setUv(0.0f, width).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0.0f, 1.0f, 0.0f);
		builder.addVertex(pose, width / 2.0f, 0.0f, width / 2.0f).setColor(CommonColors.WHITE).setUv(width, width).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0.0f, 1.0f, 0.0f);
		builder.addVertex(pose, width / 2.0f, 0.0f, -width / 2.0f).setColor(CommonColors.WHITE).setUv(width, 0.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0.0f, 1.0f, 0.0f);
	}
}
