package com.lovetropics.minigames.common.content.escape_race.misc;

import com.lovetropics.minigames.common.content.escape_race.rooms.RoomStatus;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.network.chat.Component;

public class RoomEntrancePadRenderState extends EntityRenderState {
	public float yRot;

	public float width;
	public float height;
	public float depth;
	public int ticks;

	public RoomStatus roomStatus;
	public int cost;
	public Component roomName;
	public int color = 0xFF00FF00;

	public ItemStackRenderState breakBuck = new ItemStackRenderState();


	public RoomEntrancePadRenderState() {
		super();
	}
}
