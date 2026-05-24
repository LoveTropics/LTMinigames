package com.lovetropics.minigames.common.content.escape_race.client.ddr.render;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.escape_race.client.ddr.DdrPlayerPoseState;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DDRMachineEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerCapeModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

public class DDRMachinePlayerHelper {

	private static final ContextKey<DdrPose> KEY_POSE = new ContextKey<>(LoveTropics.location("ddr/pose"));

	public static <T extends LivingEntity, S extends LivingEntityRenderState> void updateLivingEntityRenderState(T entity, S renderState) {
		if (entity.getVehicle() instanceof DDRMachineEntity ddrMachineEntity) {
			DdrPlayerPoseState poseState = ddrMachineEntity.getPoseState();
			float partialTicks = renderState.partialTick;
			renderState.setRenderData(KEY_POSE, new DdrPose(
					poseState.forward(partialTicks),
					poseState.back(partialTicks),
					poseState.left(partialTicks),
					poseState.right(partialTicks),
					ddrMachineEntity.getState() == DDRMachineEntity.DDRMachineState.BEDS
			));
			if(ddrMachineEntity.getState() == DDRMachineEntity.DDRMachineState.BEDS){
				renderState.pose = Pose.SLEEPING;
				renderState.bedOrientation = ddrMachineEntity.getDirection();
			}
		} else {
			renderState.setRenderData(KEY_POSE, null);
		}
	}

	public static void apply(LivingEntityRenderState renderState, EntityModel<?> model) {
		if (!(renderState instanceof HumanoidRenderState humanoidRenderState) || !(model instanceof HumanoidModel<?> humanoidModel)) {
			return;
		}
		DdrPose pose = humanoidRenderState.getRenderData(KEY_POSE);
		if (pose == null) {
			return;
		}

		ModelPart root = humanoidModel.root();
		ModelPart rightLeg = humanoidModel.rightLeg;
		ModelPart leftLeg = humanoidModel.leftLeg;

		if(pose.sleeping()){
			root.z = -16;
			root.y = -7;
			return;
		}

		float forward = pose.forward();
		float back = pose.back();
		float left = pose.left();
		float right = pose.right();
		float leftAndNotRight = left * (1.0f - right);
		float rightAndNotLeft = right * (1.0f - left);

		leftLeg.zRot += -58.31f * Mth.DEG_TO_RAD * remapLegAnimation(left);
		root.zRot = 0.6f * left;
		root.x += 10f * left;
		root.y += 2f * left;

		rightLeg.zRot += 58.31f * Mth.DEG_TO_RAD * remapLegAnimation(right);
		root.zRot += -0.6f * right;
		root.x -= 10f * right;
		root.y += 2f * right;

		root.xRot += 0.6f * forward;
		root.z -= 10f * forward;
		root.y += 2f * forward;
		rightLeg.xRot += -58.31f * Mth.DEG_TO_RAD * remapLegAnimation(leftAndNotRight * forward);
		leftLeg.xRot += -58.31f * Mth.DEG_TO_RAD * remapLegAnimation(rightAndNotLeft * forward);

		// Rotate the clip is it does not clip into the player model when leaning back
		if (humanoidModel instanceof PlayerCapeModel capeModel) {
			if (capeModel.body.hasChild("cape")) {
				capeModel.body.getChild("cape").xRot -= 0.5f * back;
			}
		}
		root.xRot += -0.6f * back;
		root.z += 10f * back;
		root.y += 2f * back;
		rightLeg.xRot += 58.31f * Mth.DEG_TO_RAD * remapLegAnimation(leftAndNotRight * back);
		leftLeg.xRot += 58.31f * Mth.DEG_TO_RAD * remapLegAnimation(rightAndNotLeft * back);
	}

	private static float remapLegAnimation(float factor) {
		return Math.min(1.0f, factor * 2.0f);
	}

	private record DdrPose(
			float forward,
			float back,
			float left,
			float right,
			boolean sleeping
	) {
	}
}
