package com.lovetropics.minigames.common.content.escape_race.client.ddr.render;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class DDRMachineLevelClientRenderState {
	public Component displayName = CommonComponents.EMPTY;
	public ItemStackRenderState iconState = new ItemStackRenderState();
	public boolean selected;
}
