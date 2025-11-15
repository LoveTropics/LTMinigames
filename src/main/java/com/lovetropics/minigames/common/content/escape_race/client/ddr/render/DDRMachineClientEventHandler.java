package com.lovetropics.minigames.common.content.escape_race.client.ddr.render;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DDRMachineEntity;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public class DDRMachineClientEventHandler {

	@SubscribeEvent
	public static void onRegisterLayerDefinitions(final EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(DDRMachineEntityModel.LAYER_LOCATION, DDRMachineEntityModel::createBodyLayer);
	}

	@SubscribeEvent
	public static void onRegisterRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
		event.registerEntityModifier(PlayerRenderer.class, DDRMachinePlayerHelper::updateLivingEntityRenderState);
	}

	@SubscribeEvent
	public static void onPositionCamera(ViewportEvent.ComputeCameraAngles event) {
		if(event.getCamera().getEntity() instanceof LocalPlayer localPlayer && localPlayer.getVehicle() != null && localPlayer.getVehicle() instanceof DDRMachineEntity ddrMachineEntity) {
			if(ddrMachineEntity.getState() == DDRMachineEntity.DDRMachineState.PLAYING) {
				event.setYaw(180 + ddrMachineEntity.getYRot());
				event.setPitch(45f);
				if(Minecraft.getInstance().options.getCameraType() != CameraType.THIRD_PERSON_BACK){
					Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK);
				}
			}
		}
	}

	@SubscribeEvent
	public static void onCalculateCameraDistance(CalculateDetachedCameraDistanceEvent event) {
		if(event.getCamera().getEntity() instanceof LocalPlayer localPlayer && localPlayer.getVehicle() != null && localPlayer.getVehicle() instanceof DDRMachineEntity ddrMachineEntity) {
			if(ddrMachineEntity.getState() == DDRMachineEntity.DDRMachineState.PLAYING) {
				event.setDistance(4.5f);
			}
		}
	}
}
