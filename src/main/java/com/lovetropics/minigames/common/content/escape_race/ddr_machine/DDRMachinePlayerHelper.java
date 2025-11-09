package com.lovetropics.minigames.common.content.escape_race.ddr_machine;

import com.lovetropics.minigames.LoveTropics;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerCapeModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.LivingEntity;

public class DDRMachinePlayerHelper {

	private static final ContextKey<DdrPose> KEY_POSE = new ContextKey<>(LoveTropics.location("ddr/pose"));

	public static <T extends LivingEntity, S extends LivingEntityRenderState> void updateLivingEntityRenderState(T entity, S renderState) {
		if (entity.getVehicle() instanceof DDRMachineEntity ddrMachineEntity) {
			renderState.setRenderData(KEY_POSE, new DdrPose(ddrMachineEntity.getPlayerInput()));
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
		if (pose.input.left()) {
			humanoidModel.leftLeg.zRot = -45f;
			if (!pose.input.right()) {
				humanoidModel.root().zRot = 0.6f;
				humanoidModel.root().x += 10f;
				humanoidModel.root().y += 2f;
			}
		}
		if (pose.input.right()) {
			humanoidModel.rightLeg.zRot = 45f;
			if (!pose.input.left()) {
				humanoidModel.root().zRot = -0.6f;
				humanoidModel.root().x -= 10f;
				humanoidModel.root().y += 2f;
			}
		}
		if (pose.input.forward()) {
			humanoidModel.root().xRot = 0.6f;
			humanoidModel.root().z -= 10f;
			humanoidModel.root().y += 2f;
			if (pose.input.left() && !pose.input.right()) {
				humanoidModel.rightLeg.xRot = -45f;
			} else if (!pose.input.left()) {
				humanoidModel.leftLeg.xRot = -45f;
			}
		}
		if (pose.input.back()) {
			// Rotate the clip is it does not clip into the player model when leaning back
			if (humanoidModel instanceof PlayerCapeModel<?> capeModel) {
				if (capeModel.body.hasChild("cape")) {
					capeModel.body.getChild("cape").xRot -= 0.5f;
				}
			}
			humanoidModel.root().xRot = -0.6f;
			humanoidModel.root().z += 10f;
			humanoidModel.root().y += 2f;
			if (pose.input.left() && !pose.input.right()) {
				humanoidModel.rightLeg.xRot = 45f;
			} else if (!pose.input.left()) {
				humanoidModel.leftLeg.xRot = 45f;
			}
		}
	}

	private record DdrPose(DdrInput input) {
	}
}
