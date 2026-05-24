package com.lovetropics.minigames.client.render.entity;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.render.entity.state.DriftwoodRenderState;
import com.lovetropics.minigames.common.content.survive_the_tide.entity.DriftwoodEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class DriftwoodRenderer extends EntityRenderer<DriftwoodEntity, DriftwoodRenderState> {
	private static final Identifier TEXTURE = LoveTropics.id("textures/entity/driftwood.png");

	private final DriftwoodModel model;

	public DriftwoodRenderer(EntityRendererProvider.Context context) {
		super(context);
		model = new DriftwoodModel(context.bakeLayer(DriftwoodModel.LAYER));
	}

	@Override
	public DriftwoodRenderState createRenderState() {
		return new DriftwoodRenderState();
	}

	@Override
	public void extractRenderState(DriftwoodEntity entity, DriftwoodRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.yRot = entity.getYRot(partialTick);
	}

	@Override
	public void submit(DriftwoodRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		super.submit(renderState, poseStack, submitNodeCollector, camera);

		poseStack.pushPose();
		poseStack.translate(0.0, -0.5, 0.0);
		poseStack.mulPose(Axis.YP.rotationDegrees(90.0F - renderState.yRot));

		submitNodeCollector.submitModel(model, renderState, poseStack, model.renderType(TEXTURE), renderState.lightCoords, OverlayTexture.NO_OVERLAY, renderState.outlineColor, null);

		poseStack.popPose();
	}
	@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
	public static final class DriftwoodModel extends EntityModel<EntityRenderState> {
		public static final ModelLayerLocation LAYER = new ModelLayerLocation(LoveTropics.id("driftwood"), "main");

		public DriftwoodModel(ModelPart root) {
			super(root);
		}

		@SubscribeEvent
		public static void onRegisterLayerDefinitions(final EntityRenderersEvent.RegisterLayerDefinitions event) {
			event.registerLayerDefinition(LAYER, DriftwoodModel::createBodyModel);
		}

		private static LayerDefinition createBodyModel() {
			final MeshDefinition mesh = new MeshDefinition();
			final PartDefinition root = mesh.getRoot();
			root.addOrReplaceChild("log",
					CubeListBuilder.create()
							.texOffs(0, 0)
							.addBox(-16.0F, -16.0F, -8.0F, 32.0F, 16.0F, 16.0F),
					PartPose.offset(0.0F, 24.0F, 0.0F)
			);
			return LayerDefinition.create(mesh, 128, 32);
		}
	}
}
