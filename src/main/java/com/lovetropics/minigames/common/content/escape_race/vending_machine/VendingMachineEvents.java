package com.lovetropics.minigames.common.content.escape_race.vending_machine;

import com.lovetropics.minigames.common.core.game.behavior.event.GameEventType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.item.ItemStack;

public class VendingMachineEvents {

	public static final GameEventType<PurchaseItem> PURCHASE_ITEM = GameEventType.create(VendingMachineEvents.PurchaseItem.class,
			listeners -> (player, entity, itemStack) -> {
				for (PurchaseItem listener : listeners) {
					TriState result = listener.tryPurchaseItem(player, entity, itemStack);
					if (!result.isDefault()) {
						return result;
					}
				}
				return TriState.DEFAULT;
			});

	public interface PurchaseItem {
		TriState tryPurchaseItem(ServerPlayer player, VendingMachineEntity entity, ItemStack item);
	}
}
