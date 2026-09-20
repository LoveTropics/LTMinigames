package org.lovetropics.games.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Pose;

public class ClientPoseHandler {

	public static void updateForcedPose(Pose pose) {
		if (Minecraft.getInstance().player != null) {
			Minecraft.getInstance().player.setForcedPose(pose);
		}
	}
}
