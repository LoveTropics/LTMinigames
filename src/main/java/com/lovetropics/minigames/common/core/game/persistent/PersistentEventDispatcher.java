package com.lovetropics.minigames.common.core.game.persistent;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = LoveTropics.ID)
public class PersistentEventDispatcher {
	@SubscribeEvent
	public static void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		if (!(event.getLevel() instanceof ServerLevel level)) {
			return;
		}

		for (PersistentGameInstance game : PersistentGames.in(event.getLevel())) {
			try {
				InteractionResult res = game.events().invoker(GamePlayerEvents.USE_BLOCK).onUseBlock((ServerPlayer) event.getEntity(), level, event.getPos(), event.getHand(), event.getHitVec());
				if (res == InteractionResult.CONSUME) {
					event.setUseBlock(TriState.FALSE);
				} else if (res.consumesAction()) {
					event.setCanceled(true);
					event.setCancellationResult(res);
					return;
				}
				res = game.invoker(GamePlayerEvents.USE_ITEM_ON_BLOCK).onUseBlock((ServerPlayer) event.getEntity(), level, event.getPos(), event.getHand(), event.getHitVec());
				if (res == InteractionResult.CONSUME) {
					event.setUseBlock(TriState.FALSE);
				} else if (res.consumesAction()) {
					event.setCanceled(true);
					event.setCancellationResult(res);
					return;
				}
			} catch (Exception e) {
				LoveTropics.LOGGER.warn("Failed to dispatch player use block event", e);
			}
		}
	}
}
