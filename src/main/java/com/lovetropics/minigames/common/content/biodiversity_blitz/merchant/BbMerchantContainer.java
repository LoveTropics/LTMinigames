package com.lovetropics.minigames.common.content.biodiversity_blitz.merchant;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;

public final class BbMerchantContainer extends MerchantMenu {
	public BbMerchantContainer(Merchant merchant, int id, Inventory playerInventory) {
		super(id, playerInventory, merchant);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		// copied from MerchantContainer.transferStackInSlot removing a problematic cast

		ItemStack resultStack = ItemStack.EMPTY;
		Slot slot = slots.get(index);

		if (slot != null && slot.hasItem()) {
			ItemStack slotStack = slot.getItem();
			resultStack = slotStack.copy();

			if (index == 2) {
				if (!moveItemStackTo(slotStack, 3, 39, true)) {
					return ItemStack.EMPTY;
				}

				slot.onQuickCraft(slotStack, resultStack);
			} else if (index != 0 && index != 1) {
				if (index >= 3 && index < 30) {
					if (!moveItemStackTo(slotStack, 30, 39, false)) {
						return ItemStack.EMPTY;
					}
				} else if (index >= 30 && index < 39 && !moveItemStackTo(slotStack, 3, 30, false)) {
					return ItemStack.EMPTY;
				}
			} else if (!moveItemStackTo(slotStack, 3, 39, false)) {
				return ItemStack.EMPTY;
			}

			if (slotStack.isEmpty()) {
				slot.set(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}

			if (slotStack.getCount() == resultStack.getCount()) {
				return ItemStack.EMPTY;
			}

			slot.onTake(player, slotStack);
		}

		return resultStack;
	}
}
