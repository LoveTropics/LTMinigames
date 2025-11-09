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

	private static final ContextKey<Boolean> IS_PLAYER_LEFT = new ContextKey<>(LoveTropics.location("ddr_machine_left"));
	private static final ContextKey<Boolean> IS_PLAYER_RIGHT = new ContextKey<>(LoveTropics.location("ddr_machine_right"));
	private static final ContextKey<Boolean> IS_PLAYER_FORWARD = new ContextKey<>(LoveTropics.location("ddr_machine_forward"));
	private static final ContextKey<Boolean> IS_PLAYER_BACK = new ContextKey<>(LoveTropics.location("ddr_machine_back"));
	private static final ContextKey<Boolean> IS_PLAYING_DDR = new ContextKey<>(LoveTropics.location("ddr_machine_is_playing"));

	public static <T extends LivingEntity, S extends LivingEntityRenderState> void updateLivingEntityRenderState(T entity, S renderState){
		if(entity.getVehicle() instanceof DDRMachineEntity ddrMachineEntity) {
			renderState.setRenderData(IS_PLAYING_DDR, true);
			renderState.setRenderData(IS_PLAYER_LEFT, ddrMachineEntity.isPlayerLeft());
			renderState.setRenderData(IS_PLAYER_RIGHT, ddrMachineEntity.isPlayerRight());
			renderState.setRenderData(IS_PLAYER_FORWARD, ddrMachineEntity.isPlayerForward());
			renderState.setRenderData(IS_PLAYER_BACK, ddrMachineEntity.isPlayerBack());
		} else {
			renderState.setRenderData(IS_PLAYING_DDR, false);
		}
	}

	public static void apply(LivingEntityRenderState renderState, EntityModel<?> model) {
		if (!(renderState instanceof HumanoidRenderState humanoidRenderState) || !(model instanceof HumanoidModel<?> humanoidModel)) {
			return;
		}
		if(humanoidRenderState.getRenderDataOrDefault(IS_PLAYING_DDR, false)) {
			boolean left = humanoidRenderState.getRenderDataOrDefault(IS_PLAYER_LEFT, false);
			boolean right = humanoidRenderState.getRenderDataOrDefault(IS_PLAYER_RIGHT, false);
			if(left) {
				humanoidModel.leftLeg.zRot = -45f;
				if(!right) {
					humanoidModel.root().zRot = 0.6f;
					humanoidModel.root().x += 10f;
					humanoidModel.root().y += 2f;
				}
			}
			if(right) {
				humanoidModel.rightLeg.zRot = 45f;
				if(!left) {
					humanoidModel.root().zRot = -0.6f;
					humanoidModel.root().x -= 10f;
					humanoidModel.root().y += 2f;
				}
			}
			if(humanoidRenderState.getRenderDataOrDefault(IS_PLAYER_FORWARD, false)) {
				humanoidModel.root().xRot = 0.6f;
				humanoidModel.root().z -= 10f;
				humanoidModel.root().y += 2f;
				if(left && !right){
					humanoidModel.rightLeg.xRot = -45f;
				} else if(!left){
					humanoidModel.leftLeg.xRot = -45f;
				}
			}
			if(humanoidRenderState.getRenderDataOrDefault(IS_PLAYER_BACK, false)) {
				// Rotate the clip is it does not clip into the player model when leaning back
				if (humanoidModel instanceof PlayerCapeModel<?> capeModel) {
					if(capeModel.body.hasChild("cape")) {
						capeModel.body.getChild("cape").xRot -= 0.5f;
					}
				}
				humanoidModel.root().xRot = -0.6f;
				humanoidModel.root().z += 10f;
				humanoidModel.root().y += 2f;
				if(left && !right){
					humanoidModel.rightLeg.xRot = 45f;
				} else if(!left){
					humanoidModel.leftLeg.xRot = 45f;
				}
			}
		}
	}
}
