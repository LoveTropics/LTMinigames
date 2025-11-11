package com.lovetropics.minigames.common.core.game.behavior.instances.tweak;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.PickUpResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.function.Supplier;

/**
 * Locks the inventory of players so that only the mainhand and offhand are accessible.
 * <p>
 * This effectively makes the 1st slot forced to be selected, and no other slots can be filled when an item is picked up.
 */
public record ForceHandsBehavior() implements IGameBehavior {
	public static final MapCodec<ForceHandsBehavior> CODEC = MapCodec.unit(ForceHandsBehavior::new);

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		// force items to go to first slot only
		events.listen(GamePlayerEvents.PICK_UP_ITEM, (player, item) -> {
			var inventory = player.getInventory();

			if(inventory.getItem(0).isEmpty()) {
				return PickUpResult.PASS;
			}
			return PickUpResult.CANCEL;
		});
		// force players to select first slot of the hotbar
		events.listen(GamePlayerEvents.TICK, player -> {
			if(player.getInventory().getSelectedSlot() != 0) {
				player.getInventory().setSelectedSlot(0);
				player.connection.send(new ClientboundSetHeldSlotPacket(0));
			}
		});
		// disable players from moving items around in their inventory except for offhand and mainhand
		events.listen(GamePlayerEvents.INVENTORY_CHANGED, (player, container, slotIndex, newItemStack) -> {
			if(slotIndex != InventoryMenu.USE_ROW_SLOT_START && slotIndex != InventoryMenu.SHIELD_SLOT) {
				var slot = container.getSlot(slotIndex);
				slot.tryRemove(newItemStack.getCount(), Integer.MAX_VALUE, player).ifPresent(stack -> {
					var carried = container.getCarried();
					container.setCarried(newItemStack);
					slot.onTake(player, stack);
					if(!carried.isEmpty()) {
						player.level().addFreshEntity(player.drop(carried, true));
					}
				});
				// refresh the entire inventory to prevent desync
				player.containerMenu.broadcastChanges();
			}
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.FORCE_HANDS;
	}
}
