package com.lovetropics.minigames.client.render.block.state;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.AttachFace;

public class BigRedButtonBlockEntityRenderState extends BlockEntityRenderState {

	public int presentCount;
	public int requiredCount;
	public Direction facing;
	public AttachFace face;
}
