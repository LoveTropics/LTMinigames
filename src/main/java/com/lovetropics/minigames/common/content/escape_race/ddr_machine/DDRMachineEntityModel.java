package com.lovetropics.minigames.common.content.escape_race.ddr_machine;

import com.lovetropics.minigames.LoveTropics;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class DDRMachineEntityModel extends EntityModel<DDRMachineRenderState> {

	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(LoveTropics.location("ddr_machine"), "main");
	private final ModelPart bed_rail1;
	private final ModelPart bed_rail2;
	private final ModelPart base;
	private final ModelPart hologram;
	private final ModelPart hologram2;
	private final ModelPart decor;
	private final ModelPart screen;
	private final ModelPart buttons;
	private final ModelPart up;
	private final ModelPart right;
	private final ModelPart left;
	private final ModelPart down;
	private final ModelPart bed_bottom1;
	private final ModelPart bed_bottom2;
	private final ModelPart bed_top1;
	private final ModelPart bed_top2;
	private final ModelPart bb_main;
	private final KeyframeAnimation foldIntoBedAnim;
	private final KeyframeAnimation bedToDDRAnim;

	public DDRMachineEntityModel(ModelPart root) {
		super(root);
		this.bed_rail1 = root.getChild("bed_rail1");
		this.bed_rail2 = root.getChild("bed_rail2");
		this.base = root.getChild("base");
		this.hologram = root.getChild("hologram");
		this.hologram2 = root.getChild("hologram2");
		this.decor = root.getChild("decor");
		this.screen = root.getChild("screen");
		this.buttons = root.getChild("buttons");
		this.up = this.buttons.getChild("up");
		this.right = this.buttons.getChild("right");
		this.left = this.buttons.getChild("left");
		this.down = this.buttons.getChild("down");
		this.bed_bottom1 = root.getChild("bed_bottom1");
		this.bed_bottom2 = root.getChild("bed_bottom2");
		this.bed_top1 = root.getChild("bed_top1");
		this.bed_top2 = root.getChild("bed_top2");
		this.bb_main = root.getChild("bb_main");

		foldIntoBedAnim = DDRMachineEntityModelAnimations.fold_to_bed.bake(root);
		bedToDDRAnim = DDRMachineEntityModelAnimations.bedToDDR.bake(root);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition bed_rail1 = partdefinition.addOrReplaceChild("bed_rail1", CubeListBuilder.create().texOffs(118, 103).addBox(-8.0F, -13.3333F, -1.0F, 16.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(152, 155).addBox(-8.0F, -11.3333F, -1.0F, 2.0F, 11.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(160, 155).addBox(6.0F, -11.3333F, -1.0F, 2.0F, 11.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 21.3333F, -19.0F));

		PartDefinition bed_rail2 = partdefinition.addOrReplaceChild("bed_rail2", CubeListBuilder.create().texOffs(154, 103).addBox(-8.0F, -13.0F, 0.0F, 16.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(164, 13).addBox(-8.0F, -11.0F, 0.0F, 2.0F, 11.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(164, 0).addBox(6.0F, -11.0F, 0.0F, 2.0F, 11.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 8.0F, -20.0F, 3.1416F, 0.0F, 0.0F));

		PartDefinition base = partdefinition.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 83).addBox(-24.0F, -14.0F, -2.0F, 48.0F, 14.0F, 11.0F, new CubeDeformation(0.0F))
				.texOffs(0, 39).addBox(-23.0F, -49.0F, -1.0F, 46.0F, 35.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 15.0F));

		PartDefinition hologram = partdefinition.addOrReplaceChild("hologram", CubeListBuilder.create(), PartPose.offset(0.0F, -33.2135F, 10.8897F));

		PartDefinition hologram_r1 = hologram.addOrReplaceChild("hologram_r1", CubeListBuilder.create().texOffs(110, 55).addBox(-23.0F, -11.0F, -2.0F, 46.0F, 14.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 3.2135F, 3.1103F, 0.3054F, 0.0F, 0.0F));

		PartDefinition hologram2 = partdefinition.addOrReplaceChild("hologram2", CubeListBuilder.create(), PartPose.offset(0.0F, -33.8149F, 12.7972F));

		PartDefinition hologram_r2 = hologram2.addOrReplaceChild("hologram_r2", CubeListBuilder.create().texOffs(110, 69).addBox(-23.0F, -11.0F, 0.0F, 46.0F, 14.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 3.8149F, 1.2028F, 0.3054F, 0.0F, 0.0F));

		PartDefinition decor = partdefinition.addOrReplaceChild("decor", CubeListBuilder.create().texOffs(0, 108).addBox(-18.0F, -29.0F, -2.0F, 36.0F, 29.0F, 10.0F, new CubeDeformation(0.0F))
				.texOffs(118, 87).addBox(-15.0F, -37.0F, -2.0F, 30.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
				.texOffs(136, 155).addBox(-2.0F, -45.0F, 1.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(14, 165).addBox(20.0F, -56.0F, -7.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(164, 26).addBox(20.0F, -56.0F, -12.0F, 2.0F, 2.0F, 5.0F, new CubeDeformation(0.0F))
				.texOffs(22, 165).addBox(-22.0F, -56.0F, -7.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(0, 165).addBox(-22.0F, -56.0F, -12.0F, 2.0F, 2.0F, 5.0F, new CubeDeformation(0.0F))
				.texOffs(64, 147).addBox(-2.0F, -45.0F, -2.0F, 4.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 25.0F));

		PartDefinition sign_r1 = decor.addOrReplaceChild("sign_r1", CubeListBuilder.create().texOffs(110, 39).addBox(-23.0F, -11.0F, -1.0F, 46.0F, 14.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -54.0F, -11.0F, 0.3054F, 0.0F, 0.0F));

		PartDefinition screen = partdefinition.addOrReplaceChild("screen", CubeListBuilder.create().texOffs(118, 85).addBox(-22.0F, -15.0F, -0.6F, 44.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(132, 155).addBox(21.0F, -14.0F, -0.6F, 1.0F, 28.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(128, 155).addBox(-22.0F, -14.0F, -0.6F, 1.0F, 28.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(118, 83).addBox(-22.0F, 14.0F, -0.6F, 44.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(92, 108).addBox(-21.0F, -14.0F, -0.1F, 42.0F, 28.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -9.0F, 13.6F));

		PartDefinition buttons = partdefinition.addOrReplaceChild("buttons", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 6.0F));

		PartDefinition up = buttons.addOrReplaceChild("up", CubeListBuilder.create().texOffs(2, 182).addBox(-7.0F, -1.0F, -10.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(3.0F, -3.0F, 5.0F));

		PartDefinition right = buttons.addOrReplaceChild("right", CubeListBuilder.create().texOffs(2, 192).addBox(-6.0F, -1.0F, -8.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(11.0F, -3.0F, -6.0F));

		PartDefinition left = buttons.addOrReplaceChild("left", CubeListBuilder.create().texOffs(2, 212).addBox(-7.0F, -1.0F, -8.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(-6.0F, -3.0F, -6.0F));

		PartDefinition down = buttons.addOrReplaceChild("down", CubeListBuilder.create().texOffs(2, 202).addBox(-7.0F, -1.0F, -7.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(3.0F, -3.0F, -16.0F));

		PartDefinition bed_bottom1 = partdefinition.addOrReplaceChild("bed_bottom1", CubeListBuilder.create().texOffs(90, 137).addBox(-8.0F, -1.0F, -8.0F, 16.0F, 2.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 13.0F, 23.0F));

		PartDefinition bed_bottom2 = partdefinition.addOrReplaceChild("bed_bottom2", CubeListBuilder.create().texOffs(0, 147).addBox(-8.0F, -1.0F, -8.0F, 16.0F, 2.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 13.0F, 23.0F));

		PartDefinition bed_top1 = partdefinition.addOrReplaceChild("bed_top1", CubeListBuilder.create().texOffs(64, 155).addBox(-8.0F, -1.0F, -8.0F, 16.0F, 2.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 23.0F));

		PartDefinition bed_top2 = partdefinition.addOrReplaceChild("bed_top2", CubeListBuilder.create().texOffs(154, 137).addBox(-8.0F, -1.0F, -8.0F, 16.0F, 2.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 23.0F));

		PartDefinition bb_main = partdefinition.addOrReplaceChild("bb_main", CubeListBuilder.create().texOffs(0, 0).addBox(-23.0F, -3.0F, -23.0F, 46.0F, 3.0F, 36.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 256, 256);
	}


	@Override
	public void setupAnim(DDRMachineRenderState renderState) {
		super.setupAnim(renderState);
		foldIntoBedAnim.apply(renderState.toBedState, renderState.ageInTicks, 1f);
		bedToDDRAnim.apply(renderState.toDDRState, renderState.ageInTicks, 1f);
		if(renderState.input.back()){
			down.y += .5f;
		}
		if(renderState.input.left()){
			left.y += .5f;
		}
		if(renderState.input.right()){
			right.y += .5f;
		}
		if(renderState.input.forward()){
			up.y += .5f;
		}
	}

	public ModelPart getScreen() {
		return screen;
	}
}
