package com.lovetropics.minigames.common.content.escape_race.vending_machine;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;



public class VendingMachineRenderState extends EntityRenderState {
	public float yRot;
	public NonNullList<ItemStackRenderState> items;
	public NonNullList<ItemStack> itemStacks;
	public boolean isLookingAt = false;
	public int selectedIndex = -1;
	public int selectedTicks = -1;
	public int selectedCost = -1;
	public String selectedName = "";
	public ItemStackRenderState droppingItem = new ItemStackRenderState();
	public float droppingItemProgress = 0;
	public Vector3f droppingItemStart = new Vector3f();

	public VendingMachineRenderState() {
		super();
		items = NonNullList.create();
		itemStacks = NonNullList.create();
		for (int i = 0; i < 36; i++) {
			items.add(new ItemStackRenderState());
		}
		for (int i = 0; i < 36; i++) {
			itemStacks.add(ItemStack.EMPTY.copy());
		}
	}
}
