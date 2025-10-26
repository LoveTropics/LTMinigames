package com.lovetropics.minigames.common.content.escape_race.ddr_machine;

import com.lovetropics.minigames.LoveTropics;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;


@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public class DDRMachineEntityModel extends EntityModel<DDRMachineRenderState> {

	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(LoveTropics.location("ddr_machine"), "main");
	private final ModelPart root;
	private final ModelPart handbar;
	private final ModelPart bass_speaker;
	private final ModelPart back_right_strut;
	private final ModelPart back_left_strut;
	private final ModelPart bass_speaker_top;
	private final ModelPart top;
	private final ModelPart screen_bottom_grp;
	private final ModelPart speakers;
	private final ModelPart front_right_strut;
	private final ModelPart front_left_strut;
	private final KeyframeAnimation foldIntoBedAnim;

	public DDRMachineEntityModel(ModelPart root) {
		super(root);
		this.root = root.getChild("root");
		this.handbar = root.getChild("handbar");
		this.bass_speaker = root.getChild("bass_speaker");
		this.back_right_strut = this.bass_speaker.getChild("back_right_strut");
		this.back_left_strut = this.bass_speaker.getChild("back_left_strut");
		this.bass_speaker_top = this.bass_speaker.getChild("bass_speaker_top");
		this.top = root.getChild("top");
		this.screen_bottom_grp = this.top.getChild("screen_bottom_grp");
		this.speakers = this.top.getChild("speakers");
		this.front_right_strut = this.top.getChild("front_right_strut");
		this.front_left_strut = this.top.getChild("front_left_strut");

		foldIntoBedAnim = foldIntoBed.bake(root);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create().texOffs(0, 134).addBox(-12.0F, -2.0F, -20.0F, 40.0F, 2.0F, 38.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition handbar = partdefinition.addOrReplaceChild("handbar", CubeListBuilder.create().texOffs(148, 69).addBox(-1.0F, -20.0F, 13.0F, 2.0F, 20.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(148, 91).addBox(-1.0F, -20.0F, -1.0F, 2.0F, 20.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(148, 55).addBox(-1.0F, -20.0F, 1.0F, 2.0F, 2.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offset(-11.0F, 22.0F, -8.0F));

		PartDefinition bass_speaker = partdefinition.addOrReplaceChild("bass_speaker", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -16.0F, -22.0F, 32.0F, 16.0F, 42.0F, new CubeDeformation(0.0F)), PartPose.offset(30.0F, 24.0F, 0.0F));

		PartDefinition back_right_strut = bass_speaker.addOrReplaceChild("back_right_strut", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(28.0F, -17.0F, -19.0F));

		PartDefinition back_left_strut = bass_speaker.addOrReplaceChild("back_left_strut", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(28.0F, -17.0F, 17.0F));

		PartDefinition bass_speaker_top = bass_speaker.addOrReplaceChild("bass_speaker_top", CubeListBuilder.create().texOffs(0, 0).addBox(-18.0F, -26.0F, -22.0F, 32.0F, 10.0F, 42.0F, new CubeDeformation(0.0F)), PartPose.offset(17.0F, 0.0F, 0.0F));

		PartDefinition top = partdefinition.addOrReplaceChild("top", CubeListBuilder.create().texOffs(0, 68).addBox(-1.0F, -24.0F, -22.0F, 32.0F, 14.0F, 42.0F, new CubeDeformation(0.0F)), PartPose.offset(30.0F, -2.0F, 0.0F));

		PartDefinition screen_bottom_grp = top.addOrReplaceChild("screen_bottom_grp", CubeListBuilder.create().texOffs(148, 113).addBox(-18.0F, -10.0F, -22.0F, 32.0F, 10.0F, 42.0F, new CubeDeformation(0.0F)), PartPose.offset(17.0F, 0.0F, 0.0F));

		PartDefinition speakers = top.addOrReplaceChild("speakers", CubeListBuilder.create().texOffs(148, 0).addBox(-1.0F, -13.0F, -22.0F, 17.0F, 13.0F, 42.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -24.0F, 0.0F));

		PartDefinition front_right_strut = top.addOrReplaceChild("front_right_strut", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(28.0F, -9.0F, -19.0F));

		PartDefinition front_left_strut = top.addOrReplaceChild("front_left_strut", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(28.0F, -9.0F, 17.0F));

		return LayerDefinition.create(meshdefinition, 512, 512);
	}
	@SubscribeEvent
	public static void onRegisterLayerDefinitions(final EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(LAYER_LOCATION, DDRMachineEntityModel::createBodyLayer);
	}

	@Override
	public void setupAnim(DDRMachineRenderState renderState) {
		super.setupAnim(renderState);
		foldIntoBedAnim.apply(renderState.foldAnimationState, renderState.ageInTicks, 0.5f);
	}

	public static final AnimationDefinition foldIntoBed = AnimationDefinition.Builder.withLength(3.84F)
			.addAnimation("top", new AnimationChannel(AnimationChannel.Targets.ROTATION,
					new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.52F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -180.0F), AnimationChannel.Interpolations.LINEAR)
			))
			.addAnimation("top", new AnimationChannel(AnimationChannel.Targets.POSITION,
					new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.52F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.0F, KeyframeAnimations.posVec(-2.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
			))
			.addAnimation("speakers", new AnimationChannel(AnimationChannel.Targets.ROTATION,
					new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.28F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
			))
			.addAnimation("speakers", new AnimationChannel(AnimationChannel.Targets.POSITION,
					new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.28F, KeyframeAnimations.posVec(15.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.52F, KeyframeAnimations.posVec(32.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.0F, KeyframeAnimations.posVec(32.0F, -14.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
			))
			.addAnimation("handbar", new AnimationChannel(AnimationChannel.Targets.ROTATION,
					new Keyframe(0.52F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.8F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -90.0F), AnimationChannel.Interpolations.LINEAR)
			))
			.addAnimation("handbar", new AnimationChannel(AnimationChannel.Targets.POSITION,
					new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
			))
			.addAnimation("bass_speaker_top", new AnimationChannel(AnimationChannel.Targets.POSITION,
					new Keyframe(1.48F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.72F, KeyframeAnimations.posVec(0.0F, 3.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.879F, KeyframeAnimations.posVec(0.0F, 3.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.88F, KeyframeAnimations.posVec(0.0F, 3.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(2.92F, KeyframeAnimations.posVec(0.0F, 27.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
			))
			.addAnimation("screen_bottom_grp", new AnimationChannel(AnimationChannel.Targets.POSITION,
					new Keyframe(1.48F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.72F, KeyframeAnimations.posVec(0.0F, -3.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.879F, KeyframeAnimations.posVec(0.0F, -3.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.88F, KeyframeAnimations.posVec(0.0F, -3.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(2.92F, KeyframeAnimations.posVec(0.0F, -27.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
			))
			.addAnimation("back_right_strut", new AnimationChannel(AnimationChannel.Targets.POSITION,
					new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.48F, KeyframeAnimations.posVec(0.0F, -1.97F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.72F, KeyframeAnimations.posVec(0.0F, 0.03F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.879F, KeyframeAnimations.posVec(0.0F, 0.03F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.88F, KeyframeAnimations.posVec(0.0F, 0.03F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(2.92F, KeyframeAnimations.posVec(0.0F, 12.03F, 0.0F), AnimationChannel.Interpolations.LINEAR)
			))
			.addAnimation("back_right_strut", new AnimationChannel(AnimationChannel.Targets.SCALE,
					new Keyframe(1.48F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.72F, KeyframeAnimations.scaleVec(1.0F, 2.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.879F, KeyframeAnimations.scaleVec(1.0F, 2.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.88F, KeyframeAnimations.scaleVec(1.0F, 2.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(2.92F, KeyframeAnimations.scaleVec(1.0F, 15.2F, 1.0F), AnimationChannel.Interpolations.LINEAR)
			))
			.addAnimation("back_left_strut", new AnimationChannel(AnimationChannel.Targets.POSITION,
					new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.48F, KeyframeAnimations.posVec(0.0F, -1.97F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.72F, KeyframeAnimations.posVec(0.0F, 0.03F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.879F, KeyframeAnimations.posVec(0.0F, 0.03F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.88F, KeyframeAnimations.posVec(0.0F, 0.03F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(2.92F, KeyframeAnimations.posVec(0.0F, 12.03F, 0.0F), AnimationChannel.Interpolations.LINEAR)
			))
			.addAnimation("back_left_strut", new AnimationChannel(AnimationChannel.Targets.SCALE,
					new Keyframe(1.48F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.72F, KeyframeAnimations.scaleVec(1.0F, 2.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.879F, KeyframeAnimations.scaleVec(1.0F, 2.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.88F, KeyframeAnimations.scaleVec(1.0F, 2.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(2.92F, KeyframeAnimations.scaleVec(1.0F, 15.2F, 1.0F), AnimationChannel.Interpolations.LINEAR)
			))
			.addAnimation("front_right_strut", new AnimationChannel(AnimationChannel.Targets.POSITION,
					new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.48F, KeyframeAnimations.posVec(0.0F, -1.97F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.72F, KeyframeAnimations.posVec(0.0F, 0.03F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.879F, KeyframeAnimations.posVec(0.0F, 0.03F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.88F, KeyframeAnimations.posVec(0.0F, 0.03F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(2.92F, KeyframeAnimations.posVec(0.0F, -10.97F, 0.0F), AnimationChannel.Interpolations.LINEAR)
			))
			.addAnimation("front_right_strut", new AnimationChannel(AnimationChannel.Targets.SCALE,
					new Keyframe(1.48F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.72F, KeyframeAnimations.scaleVec(1.0F, 2.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.879F, KeyframeAnimations.scaleVec(1.0F, 2.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.88F, KeyframeAnimations.scaleVec(1.0F, 2.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(2.92F, KeyframeAnimations.scaleVec(1.0F, 15.2F, 1.0F), AnimationChannel.Interpolations.LINEAR)
			))
			.addAnimation("front_left_strut", new AnimationChannel(AnimationChannel.Targets.POSITION,
					new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.48F, KeyframeAnimations.posVec(0.0F, -1.97F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.72F, KeyframeAnimations.posVec(0.0F, 0.03F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.879F, KeyframeAnimations.posVec(0.0F, 0.03F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.88F, KeyframeAnimations.posVec(0.0F, 0.03F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(2.4F, KeyframeAnimations.posVec(0.0F, -10.97F, 0.0F), AnimationChannel.Interpolations.LINEAR)
			))
			.addAnimation("front_left_strut", new AnimationChannel(AnimationChannel.Targets.SCALE,
					new Keyframe(1.48F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.72F, KeyframeAnimations.scaleVec(1.0F, 2.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.879F, KeyframeAnimations.scaleVec(1.0F, 2.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.88F, KeyframeAnimations.scaleVec(1.0F, 2.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(2.4F, KeyframeAnimations.scaleVec(1.0F, 15.2F, 1.0F), AnimationChannel.Interpolations.LINEAR)
			))
			.build();
}
