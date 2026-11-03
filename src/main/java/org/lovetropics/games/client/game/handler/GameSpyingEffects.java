package org.lovetropics.games.client.game.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.client.game.ClientGameStateManager;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;

@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public final class GameSpyingEffects {
	private static boolean isSpying() {
		return ClientGameStateManager.getOrNull(GameClientStateTypes.SPYING) != null;
	}

	@SubscribeEvent
	public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player != null && isSpying()) {
			float partialTick = (float) event.getPartialTick();
			event.setYaw(player.getViewYRot(partialTick));
			event.setPitch(player.getViewXRot(partialTick));
		}
	}
}
