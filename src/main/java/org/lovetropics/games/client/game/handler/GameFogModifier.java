package org.lovetropics.games.client.game.handler;

import org.lovetropics.games.client.game.ClientGameStateManager;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import org.lovetropics.games.common.core.game.client_state.instance.FogClientState;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(Dist.CLIENT)
public class GameFogModifier {
	@SubscribeEvent(priority = EventPriority.HIGH)
	static void onModifyFog(ViewportEvent.ComputeFogColor event) {
		FogClientState state = ClientGameStateManager.getOrNull(GameClientStateTypes.FOG);
		if (state == null) {
			return;
		}

		event.setRed(state.red());
		event.setGreen(state.green());
		event.setBlue(state.blue());
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	static void onRenderFog(ViewportEvent.RenderFog event) {
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
