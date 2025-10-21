package com.lovetropics.minigames.client.render.special;

import com.lovetropics.minigames.LoveTropics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;

@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public class MinigameSpecialRenderers {
	@SubscribeEvent
	public static void register(RegisterSpecialModelRendererEvent event) {
		event.register(LoveTropics.location("mob_item"), MobItemSpecialRenderer.Unbaked.MAP_CODEC);
	}
}
