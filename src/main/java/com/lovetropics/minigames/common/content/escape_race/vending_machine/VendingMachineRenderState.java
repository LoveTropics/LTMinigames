package com.lovetropics.minigames.common.content.escape_race.vending_machine;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class VendingMachineRenderState extends EntityRenderState {
	public float yRot;
	public NonNullList<ItemStackRenderState> items;
	public boolean isLookingAt = false;

	public VendingMachineRenderState() {
		super();
		items = NonNullList.create();
		for (int i = 0; i < 36; i++) {
			items.add(new ItemStackRenderState());
		}
	}
}
