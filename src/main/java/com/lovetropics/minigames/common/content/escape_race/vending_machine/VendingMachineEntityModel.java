package com.lovetropics.minigames.common.content.escape_race.vending_machine;

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
import net.neoforged.neoforge.client.event.FrameGraphSetupEvent;

@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public class VendingMachineEntityModel extends EntityModel<VendingMachineRenderState> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(LoveTropics.location("vending_machine"), "main");
	private final ModelPart root;
	private final ModelPart root2;
	private final ModelPart machine;
	private final ModelPart vending_flap;
	private final ModelPart control_panel;
	private final ModelPart buy_button;
	private final ModelPart buy_button_on;
	private final ModelPart price_screen_cover;

	public static final AnimationDefinition scale_buy_button = AnimationDefinition.Builder.withLength(0.0F).looping()
		.addAnimation("control_panel", new AnimationChannel(AnimationChannel.Targets.POSITION,
			new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 4.0F, 2.0F), AnimationChannel.Interpolations.LINEAR)
			))
			.addAnimation("control_panel", new AnimationChannel(AnimationChannel.Targets.SCALE,
			new Keyframe(0.0F, KeyframeAnimations.scaleVec(0.3333F, 0.3333F, 0.3333F), AnimationChannel.Interpolations.LINEAR)
			))
			.build();

	private final KeyframeAnimation scale_buy_button_on;


	public VendingMachineEntityModel(ModelPart root) {
		super(root);
		this.root = root.getChild("root");
		this.root2 = this.root.getChild("root2");
		this.machine = this.root2.getChild("machine");
		this.vending_flap = this.machine.getChild("vending_flap");
		this.control_panel = this.machine.getChild("control_panel");
		this.buy_button = this.control_panel.getChild("buy_button");
		this.buy_button_on = this.buy_button.getChild("buy_button_on");
		this.price_screen_cover = this.control_panel.getChild("price_screen_cover");
		scale_buy_button_on = scale_buy_button.bake(root);
		buy_button_on.visible = false;
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 1.0F));

		PartDefinition root2 = root.addOrReplaceChild("root2", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition machine = root2.addOrReplaceChild("machine", CubeListBuilder.create().texOffs(108, 21).addBox(11.0F, -2.0F, -8.0F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(108, 21).addBox(-14.0F, -2.0F, -8.0F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(108, 21).addBox(-14.0F, -2.0F, 3.0F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(108, 21).addBox(11.0F, -2.0F, 3.0F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(0, 62).addBox(-12.0F, -48.0F, 6.0F, 20.0F, 46.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(102, 103).addBox(-12.0F, -48.0F, -9.0F, 20.0F, 10.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(102, 77).addBox(-12.0F, -38.0F, -8.0F, 20.0F, 26.0F, 0.0F, new CubeDeformation(0.0F))
				.texOffs(84, 55).addBox(-12.0F, -6.0F, -9.0F, 20.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(108, 26).addBox(-12.0F, -10.0F, -9.0F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(108, 26).addBox(4.0F, -10.0F, -9.0F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(108, 0).addBox(-12.0F, -12.0F, -9.0F, 20.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(42, 62).addBox(-12.0F, -3.0F, -8.0F, 20.0F, 1.0F, 14.0F, new CubeDeformation(0.0F))
				.texOffs(110, 62).addBox(-12.0F, -48.0F, -8.0F, 20.0F, 1.0F, 14.0F, new CubeDeformation(0.0F))
				.texOffs(46, 0).addBox(-15.0F, -48.0F, -9.0F, 3.0F, 46.0F, 16.0F, new CubeDeformation(0.0F))
				.texOffs(0, 0).addBox(8.0F, -48.0F, -9.0F, 7.0F, 46.0F, 16.0F, new CubeDeformation(0.0F))
				.texOffs(108, 3).addBox(3.0F, -33.0F, -5.0F, 1.0F, 2.0F, 11.0F, new CubeDeformation(0.0F))
				.texOffs(108, 3).addBox(-2.0F, -33.0F, -5.0F, 1.0F, 2.0F, 11.0F, new CubeDeformation(0.0F))
				.texOffs(108, 3).addBox(-7.0F, -33.0F, -5.0F, 1.0F, 2.0F, 11.0F, new CubeDeformation(0.0F))
				.texOffs(42, 99).addBox(-11.0F, -31.0F, -5.0F, 19.0F, 1.0F, 11.0F, new CubeDeformation(0.0F))
				.texOffs(108, 3).addBox(3.0F, -27.0F, -5.0F, 1.0F, 2.0F, 11.0F, new CubeDeformation(0.0F))
				.texOffs(108, 3).addBox(-2.0F, -27.0F, -5.0F, 1.0F, 2.0F, 11.0F, new CubeDeformation(0.0F))
				.texOffs(108, 3).addBox(-7.0F, -27.0F, -5.0F, 1.0F, 2.0F, 11.0F, new CubeDeformation(0.0F))
				.texOffs(42, 99).addBox(-11.0F, -25.0F, -5.0F, 19.0F, 1.0F, 11.0F, new CubeDeformation(0.0F))
				.texOffs(108, 3).addBox(3.0F, -21.0F, -5.0F, 1.0F, 2.0F, 11.0F, new CubeDeformation(0.0F))
				.texOffs(108, 3).addBox(-2.0F, -21.0F, -5.0F, 1.0F, 2.0F, 11.0F, new CubeDeformation(0.0F))
				.texOffs(108, 3).addBox(-7.0F, -21.0F, -5.0F, 1.0F, 2.0F, 11.0F, new CubeDeformation(0.0F))
				.texOffs(42, 99).addBox(-11.0F, -19.0F, -5.0F, 19.0F, 1.0F, 11.0F, new CubeDeformation(0.0F))
				.texOffs(108, 3).addBox(-7.0F, -15.0F, -5.0F, 1.0F, 2.0F, 11.0F, new CubeDeformation(0.0F))
				.texOffs(84, 0).addBox(-12.0F, -47.0F, -5.0F, 1.0F, 44.0F, 11.0F, new CubeDeformation(0.0F))
				.texOffs(108, 3).addBox(-2.0F, -15.0F, -5.0F, 1.0F, 2.0F, 11.0F, new CubeDeformation(0.0F))
				.texOffs(108, 3).addBox(3.0F, -15.0F, -5.0F, 1.0F, 2.0F, 11.0F, new CubeDeformation(0.0F))
				.texOffs(42, 77).addBox(-11.0F, -47.0F, -5.0F, 19.0F, 11.0F, 11.0F, new CubeDeformation(0.0F))
				.texOffs(42, 99).addBox(-11.0F, -13.0F, -5.0F, 19.0F, 1.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition vending_flap = machine.addOrReplaceChild("vending_flap", CubeListBuilder.create().texOffs(108, 16).addBox(-6.0F, 0.0F, -9.0F, 12.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, -10.0F, 1.0F));

		PartDefinition control_panel = machine.addOrReplaceChild("control_panel", CubeListBuilder.create().texOffs(10, 122).addBox(-21.0F, -45.0F, 0.0F, 21.0F, 45.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(15.0F, -13.0F, -12.0F));

		PartDefinition buy_button = control_panel.addOrReplaceChild("buy_button", CubeListBuilder.create().texOffs(68, 154).addBox(2.0F, -12.0F, -33.0F, 15.0F, 9.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-20.0F, 0.0F, 32.0F));

		PartDefinition buy_button_on = buy_button.addOrReplaceChild("buy_button_on", CubeListBuilder.create().texOffs(69, 125).addBox(2.0F, -12.0F, -33.0313F, 15.0F, 9.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition price_screen_cover = control_panel.addOrReplaceChild("price_screen_cover", CubeListBuilder.create().texOffs(68, 169).addBox(-7.5F, -3.0F, -1.5F, 15.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-10.5F, -39.0F, 0.5F));

		return LayerDefinition.create(meshdefinition, 256, 256);
	}

	@SubscribeEvent
	public static void onRegisterLayerDefinitions(final EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(LAYER_LOCATION, VendingMachineEntityModel::createBodyLayer);
	}

	@Override
	public void setupAnim(VendingMachineRenderState renderState) {
		super.setupAnim(renderState);
		scale_buy_button_on.applyStatic();
		buy_button_on.visible = renderState.selectedIndex != -1;
		if(renderState.selectedIndex != -1) {
			buy_button.z -= 1;
		}
	}
}
