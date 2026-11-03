package org.lovetropics.games.client.game.handler;

import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.client.game.ClientGameStateManager;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;

@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public final class GameSpyingEffects {
	private static final Identifier POST_EFFECT = LoveTropics.id("spying");
	private static final CrossFrameResourcePool RESOURCE_POOL = new CrossFrameResourcePool(3);

	private static boolean isSpying() {
		return ClientGameStateManager.getOrNull(GameClientStateTypes.SPYING) != null;
	}

	// It's Post so we apply to screens too
	@SubscribeEvent
	public static void onRenderFrame(RenderFrameEvent.Post event) {

		if (!isSpying()) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		PostChain postChain = minecraft.getShaderManager().getPostChain(POST_EFFECT, LevelTargetBundle.MAIN_TARGETS);
		if (postChain != null) {
			postChain.process(minecraft.gameRenderer.mainRenderTarget(), RESOURCE_POOL);
			RESOURCE_POOL.endFrame();
		}
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
