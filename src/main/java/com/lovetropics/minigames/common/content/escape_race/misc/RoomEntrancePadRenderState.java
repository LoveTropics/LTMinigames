package com.lovetropics.minigames.common.content.escape_race.misc;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class RoomEntrancePadRenderState extends EntityRenderState {
	public float yRot;

	public float width;
	public float height;
	public float depth;
	public int ticks;

	public int cost;
	public Component roomName = CommonComponents.EMPTY;
	public int color = 0xFF00FF00;
	public boolean canAfford;

	public ItemStackRenderState breakBuck = new ItemStackRenderState();


	public RoomEntrancePadRenderState() {
		super();
	}
}
