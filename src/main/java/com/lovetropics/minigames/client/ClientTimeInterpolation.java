package com.lovetropics.minigames.client;

import com.lovetropics.minigames.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Constants.MODID, value = Dist.CLIENT)
public final class ClientTimeInterpolation {
	private static int speed = 0;

	public static void updateSpeed(int newSpeed) {
		speed = newSpeed;
	}

	public static void handleTick(ClientWorld world) {
		if (speed != 0 && !isTimePaused()) {
			world.setDayTime(world.getDayTime() + speed - 1);
		}
	}

	private static boolean isTimePaused() {
		return Minecraft.getInstance().isGamePaused() && Minecraft.getInstance().isSingleplayer();
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase == TickEvent.Phase.START) return;

		ClientWorld world = Minecraft.getInstance().world;

		if (world != null) {
			handleTick(world);
		}
	}

	@SubscribeEvent
	public static void onPlayerLoggedOutClient(ClientPlayerNetworkEvent.LoggedOutEvent event) {
		// Reset speed on logged out so that players don't get issues with phantom time smearing in places where it doesn't make sense
		updateSpeed(0);
	}
}
