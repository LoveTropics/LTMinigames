package com.lovetropics.minigames.client.game.handler;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.game.ClientGameStateManager;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import org.jetbrains.annotations.UnknownNullability;
import org.spongepowered.asm.mixin.injection.At;

@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public final class HealthTagRenderer {
	private static final SpriteId HEART_CONTAINER_SPRITE = new SpriteId(AtlasIds.GUI, Identifier.withDefaultNamespace("hud/heart/container"));
	private static final SpriteId HEART_SPRITE = new SpriteId(AtlasIds.GUI, Identifier.withDefaultNamespace("hud/heart/full"));

	private static final ContextKey<Float> HEALTH_TAG_KEY = new ContextKey<>(LoveTropics.location("health_tag"));

	@SubscribeEvent
	public static void onRegisterRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
		event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
			@Override
			public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState renderState) {
				if (avatar instanceof Player player) {
					Minecraft minecraft = Minecraft.getInstance();

					if (!player.isCreative() && !player.isSpectator()) {
						if (player == minecraft.getCameraEntity() || !Minecraft.renderNames()) {
							return;
						}

						double distanceSq = minecraft.getEntityRenderDispatcher().distanceToSqr(player);
						if (!ClientHooks.isNameplateInRenderDistance(player, distanceSq) || player.isDiscrete()) {
							return;
						}

						if (ClientGameStateManager.getOrNull(GameClientStateTypes.HEALTH_TAG) != null) {
							renderState.setRenderData(HEALTH_TAG_KEY, player.getHealth() / player.getMaxHealth());
						}
					}
				}
			}
		});
	}

	@SubscribeEvent
	public static void onRenderPlayerName(RenderPlayerEvent.Post<AbstractClientPlayer> event) {
		Float healthPercent = event.getRenderState().getRenderData(HEALTH_TAG_KEY);
		if (healthPercent == null) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		renderHealthTag(minecraft, event.getPoseStack(), event.getSubmitNodeCollector(), event.getRenderState(), healthPercent);
	}

	private static void renderHealthTag(Minecraft minecraft, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, LivingEntityRenderState renderState, float healthPercent) {
		String healthText = (int) (healthPercent * 100.0f) + "%";

		Font font = minecraft.font;

		final float iconSize = 8.0F;
		final float textScale = 1.0F / 2.5F;
		float left = -(font.width(healthText) * textScale + iconSize) / 2.0F;

		poseStack.pushPose();
		poseStack.translate(0.0, renderState.boundingBoxHeight + 0.75, 0.0);
		poseStack.mulPose(minecraft.getEntityRenderDispatcher().camera.rotation());
		poseStack.scale(-textScale / 16.0f, -textScale / 16.0f, textScale / 16.0f);

		float textX = (left + iconSize) * textScale;
		float textY = -font.lineHeight / 2.0F;
		poseStack.pushPose();
		poseStack.scale(-1, 1, 1);
		poseStack.translate(-16, 0, 0);
		submitNodeCollector
				.submitText(
						poseStack,
						textX,
						textY,
						FormattedCharSequence.forward(healthText, Style.EMPTY),
						false,
						Font.DisplayMode.NORMAL,
						renderState.lightCoords,
						CommonColors.WHITE,
						0,
						renderState.outlineColor);
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(left - 4.5f, -4.5F, 0.0F);
		drawSprite(poseStack, submitNodeCollector, HEART_CONTAINER_SPRITE, 9, 9);
		drawSprite(poseStack, submitNodeCollector, HEART_SPRITE, 9, 9);
		poseStack.popPose();

		poseStack.popPose();
	}
//
	private static void drawSprite(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, SpriteId spriteId, int width, int height) {
		PoseStack.Pose pose = poseStack.last();
		TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().get(spriteId);
		submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.text(sprite.atlasLocation()), (pose1, consumer) -> {
			consumer.addVertex(pose, 0.0f, 0.0f, 0.0f).setUv(sprite.getU0(), sprite.getV0()).setColor(CommonColors.WHITE).setLight(LightCoordsUtil.FULL_BRIGHT);
			consumer.addVertex(pose, width, 0.0f, 0.0f).setUv(sprite.getU1(), sprite.getV0()).setColor(CommonColors.WHITE).setLight(LightCoordsUtil.FULL_BRIGHT);
			consumer.addVertex(pose, width, height, 0.0f).setUv(sprite.getU1(), sprite.getV1()).setColor(CommonColors.WHITE).setLight(LightCoordsUtil.FULL_BRIGHT);
			consumer.addVertex(pose, 0.0f, height, 0.0f).setUv(sprite.getU0(), sprite.getV1()).setColor(CommonColors.WHITE).setLight(LightCoordsUtil.FULL_BRIGHT);
		});


	}
}
