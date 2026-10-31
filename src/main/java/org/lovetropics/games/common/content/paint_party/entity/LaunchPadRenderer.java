package org.lovetropics.games.common.content.paint_party.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.breeze.BreezeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.BreezeRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;

public class LaunchPadRenderer extends EntityRenderer<LaunchPad, BreezeRenderState> {
	private static final Identifier WIND_TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/entity/breeze/breeze_wind.png");

	private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucent(Identifier.withDefaultNamespace("textures/entity/breeze/breeze.png"));
	private static final RenderType RENDER_TYPE_EYES = RenderTypes.breezeEyes(Identifier.withDefaultNamespace("textures/entity/breeze/breeze_eyes.png"));

	private final EntityModel<BreezeRenderState> model;
	private final EntityModel<BreezeRenderState> windModel;
	private final EntityModel<BreezeRenderState> eyesModel;

	public LaunchPadRenderer(EntityRendererProvider.Context context) {
		super(context);
		model = new BreezeModel(context.bakeLayer(ModelLayers.BREEZE));
		windModel = new BreezeModel(context.bakeLayer(ModelLayers.BREEZE_WIND));
		eyesModel = new BreezeModel(context.bakeLayer(ModelLayers.BREEZE_EYES));
	}

	@Override
	public void submit(BreezeRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		poseStack.pushPose();

		poseStack.scale(state.scale, state.scale, state.scale);
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - state.bodyRot));
		poseStack.scale(-1.0f, -1.0f, 1.0f);
		poseStack.translate(0.0f, EntityModel.MODEL_Y_OFFSET, 0.0f);

		submitNodeCollector.submitModel(model, state, poseStack, RENDER_TYPE, state.lightCoords, OverlayTexture.NO_OVERLAY, CommonColors.WHITE, null, state.outlineColor, null);

		RenderType windRenderType = RenderTypes.breezeWind(WIND_TEXTURE_LOCATION, state.ageInTicks * 0.02f % 1.0f, 0.0f);
		submitNodeCollector.order(1).submitModel(windModel, state, poseStack, windRenderType, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);

		submitNodeCollector.order(1).submitModel(eyesModel, state, poseStack, RENDER_TYPE_EYES, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);

		poseStack.popPose();
		super.submit(state, poseStack, submitNodeCollector, camera);
	}

	@Override
	public BreezeRenderState createRenderState() {
		return new BreezeRenderState();
	}

	@Override
	public void extractRenderState(LaunchPad entity, BreezeRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.bodyRot = entity.getYRot(partialTicks);
		state.scale *= 0.4f;
	}
}
