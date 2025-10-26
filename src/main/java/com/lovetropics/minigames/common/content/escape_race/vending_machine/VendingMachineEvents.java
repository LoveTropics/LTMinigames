package com.lovetropics.minigames.common.content.escape_race.vending_machine;

import com.lovetropics.minigames.common.core.game.behavior.event.GameEventType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class VendingMachineEvents {

	public static final GameEventType<PurchaseItem> PURCHASE_ITEM = GameEventType.create(VendingMachineEvents.PurchaseItem.class,
			listeners -> (player, entity, itemStack) -> {
				for (PurchaseItem listener : listeners) {
					boolean isCorrect = listener.onPurchaseItem(player, entity, itemStack);
					if (isCorrect) {
						return true;
					}
				}
				return false;
			});

	public interface PurchaseItem {
		boolean onPurchaseItem(Player player, VendingMachineEntity entity,
							   ItemStack item);
	}

}
