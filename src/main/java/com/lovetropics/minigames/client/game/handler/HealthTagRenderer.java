package com.lovetropics.minigames.client.game.handler;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.game.ClientGameStateManager;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CommonColors;
import net.minecraft.util.context.ContextKey;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public final class HealthTagRenderer {
	private static final ResourceLocation HEART_CONTAINER_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/container");
	private static final ResourceLocation HEART_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/full");

	private static final ContextKey<Float> HEALTH_TAG_KEY = new ContextKey<>(LoveTropics.location("health_tag"));

	@SubscribeEvent
	public static void onRegisterRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
		event.registerEntityModifier(PlayerRenderer.class, (player, state) -> {
			Minecraft minecraft = Minecraft.getInstance();

			if (!player.isCreative() && !player.isSpectator()) {
				if (player == minecraft.cameraEntity || !Minecraft.renderNames()) {
					return;
				}

				double distanceSq = minecraft.getEntityRenderDispatcher().distanceToSqr(player);
				if (!ClientHooks.isNameplateInRenderDistance(player, distanceSq) || player.isDiscrete()) {
					return;
				}

				if (ClientGameStateManager.getOrNull(GameClientStateTypes.HEALTH_TAG) != null) {
					state.setRenderData(HEALTH_TAG_KEY, player.getHealth() / player.getMaxHealth());
				}
			}
		});
	}

	@SubscribeEvent
	public static void onRenderPlayerName(RenderPlayerEvent.Post event) {
		Float healthPercent = event.getRenderState().getRenderData(HEALTH_TAG_KEY);
		if (healthPercent == null) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		renderHealthTag(minecraft, event.getPoseStack(), event.getMultiBufferSource(), event.getRenderState(), healthPercent);
	}

	private static void renderHealthTag(Minecraft minecraft, PoseStack poseStack, MultiBufferSource bufferSource, PlayerRenderState renderState, float healthPercent) {
		String healthText = (int) (healthPercent * 100.0f) + "%";

		Font font = minecraft.font;

		final float iconSize = 8.0F;
		final float textScale = 1.0F / 2.5F;
		float left = -(font.width(healthText) * textScale + iconSize) / 2.0F;

		poseStack.pushPose();
		poseStack.translate(0.0, renderState.boundingBoxHeight + 0.75, 0.0);
		poseStack.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
		poseStack.scale(-textScale / 16.0f, -textScale / 16.0f, textScale / 16.0f);

		float textX = (left + iconSize) * textScale;
		float textY = -font.lineHeight / 2.0F;
		poseStack.pushPose();
		poseStack.scale(-1, 1, 1);
		poseStack.translate(-16, 0, 0);
		font.drawInBatch(healthText, textX, textY, CommonColors.WHITE, false, poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(left - 4.5f, -4.5F, 0.0F);
		drawSprite(poseStack, bufferSource, HEART_CONTAINER_SPRITE, 9, 9);
		drawSprite(poseStack, bufferSource, HEART_SPRITE, 9, 9);
		poseStack.popPose();

		poseStack.popPose();
	}

	private static void drawSprite(PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation spriteId, int width, int height) {
		PoseStack.Pose pose = poseStack.last();
		TextureAtlasSprite sprite = Minecraft.getInstance().getGuiSprites().getSprite(spriteId);
		VertexConsumer buffer = bufferSource.getBuffer(RenderType.text(sprite.atlasLocation()));
		buffer.addVertex(pose, 0.0f, 0.0f, 0.0f).setUv(sprite.getU0(), sprite.getV0()).setColor(CommonColors.WHITE).setLight(LightTexture.FULL_BRIGHT);
		buffer.addVertex(pose, width, 0.0f, 0.0f).setUv(sprite.getU1(), sprite.getV0()).setColor(CommonColors.WHITE).setLight(LightTexture.FULL_BRIGHT);
		buffer.addVertex(pose, width, height, 0.0f).setUv(sprite.getU1(), sprite.getV1()).setColor(CommonColors.WHITE).setLight(LightTexture.FULL_BRIGHT);
		buffer.addVertex(pose, 0.0f, height, 0.0f).setUv(sprite.getU0(), sprite.getV1()).setColor(CommonColors.WHITE).setLight(LightTexture.FULL_BRIGHT);
	}
}
