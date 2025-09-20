package com.lovetropics.minigames.client.game.handler.spectate;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.game.handler.ClientGameStateHandler;
import com.lovetropics.minigames.common.core.game.client_state.instance.SpectatingClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.spectator.SpectatorGui;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

import javax.annotation.Nullable;
import java.util.UUID;

@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public final class ClientSpectatingManager implements ClientGameStateHandler<SpectatingClientState> {
	public static final ClientSpectatingManager INSTANCE = new ClientSpectatingManager();

	static final double MAX_CHASE_DISTANCE = 16.0;

	@Nullable
	SpectatingSession session;

	@Override
	public void accept(SpectatingClientState state) {
		SpectatingSession session = this.session;
		if (session == null) {
			this.session = new SpectatingSession(state.players());
		} else {
			session.updatePlayers(state.players());
		}
	}

	@Override
	public void disable(SpectatingClientState state) {
		SpectatingSession session = this.session;
		this.session = null;

		if (session != null) {
			session.close();
		}
	}

	public void onPlayerActivity(UUID player, int color) {
		if (session != null) {
			session.ui.onPlayerActivity(player, color);
		}
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player != null) {
			SpectatingSession session = INSTANCE.session;
			if (session != null) {
				session.tick();

				// keep the vanilla spectator gui closed
				SpectatorGui spectatorGui = minecraft.gui.getSpectatorGui();
				spectatorGui.onSpectatorMenuClosed(null);
			}
		}
	}

	@SubscribeEvent
	public static void onRenderTick(RenderFrameEvent.Pre event) {
		if (Minecraft.getInstance().player != null) {
			SpectatingSession session = INSTANCE.session;
			if (session != null) {
				session.renderTick();
			}
		}
	}

	@SubscribeEvent
	public static void onPositionCamera(ViewportEvent.ComputeCameraAngles event) {
		SpectatingSession session = INSTANCE.session;
		if (session != null) {
			session.applyToCamera(event.getCamera(), (float) event.getPartialTick(), event);
		}
	}

	@SubscribeEvent
	public static void onCalculateCameraDistance(CalculateDetachedCameraDistanceEvent event) {
		SpectatingSession session = INSTANCE.session;
		if (session != null) {
			float partialTicks = event.getCamera().getPartialTickTime();
			session.applyCameraDistance(event.getCamera(), partialTicks, event);
		}
	}
}
