package com.lovetropics.minigames.client.game.handler;

import com.lovetropics.minigames.client.game.ClientGameStateManager;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.lovetropics.minigames.common.core.game.client_state.instance.FogClientState;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(Dist.CLIENT)
public class GameFogModifier {
	@SubscribeEvent(priority = EventPriority.HIGH)
	static void onModifyFog(final ViewportEvent.ComputeFogColor event) {
		FogClientState state = ClientGameStateManager.getOrNull(GameClientStateTypes.FOG);
		if (state == null) {
			return;
		}

		event.setRed(state.red());
		event.setGreen(state.green());
		event.setBlue(state.blue());
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	static void onRenderFog(final ViewportEvent.RenderFog event) {
		if (event.getType() != FogType.ATMOSPHERIC) {
			return;
		}

		FogClientState state = ClientGameStateManager.getOrNull(GameClientStateTypes.FOG);
		if (state == null) {
			return;
		}

		event.setNearPlaneDistance(Math.min(state.nearDistance(), event.getNearPlaneDistance()));
		event.setFarPlaneDistance(Math.min(state.farDistance(), event.getFarPlaneDistance()));
	}
}
