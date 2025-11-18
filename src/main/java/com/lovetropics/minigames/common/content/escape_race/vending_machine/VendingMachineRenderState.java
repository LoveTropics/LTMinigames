package com.lovetropics.minigames.common.content.escape_race.vending_machine;

import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class VendingMachineRenderState extends EntityRenderState {
	public float yRot;
	public final List<SlotState> slots = new ArrayList<>(VendingMachineSlots.COUNT);
	public boolean hasSelection;
	public boolean buyButtonPicked;

	public final ItemStackRenderState droppingItem = new ItemStackRenderState();
	public int droppingFromSlot = VendingMachineEntity.NO_SLOT;
	public float droppingItemProgress = 0.0f;

	public VendingMachineRenderState() {
		super();
		for (VendingMachineSlot slot : VendingMachineSlots.SLOTS) {
			slots.add(new SlotState(slot.pos()));
		}
	}

	public static class SlotState {
		public final Vec3 pos;
		public final ItemStackRenderState item = new ItemStackRenderState();
		public boolean selected;
		public boolean picked;
		public Component name = CommonComponents.EMPTY;
		public int cost;

		public SlotState(Vec3 pos) {
			this.pos = pos;
		}

		public void update(ItemModelResolver itemModelResolver, ItemStack itemStack, VendingMachineEntity entity, boolean selected, boolean picked) {
			itemModelResolver.updateForNonLiving(item, itemStack, ItemDisplayContext.FIXED, entity);
			this.selected = selected;
			this.picked = picked;
			name = itemStack.getHoverName();
			cost = itemStack.getOrDefault(EscapeRace.VENDING_MACHINE_COST, 0);
		}
	}
}
