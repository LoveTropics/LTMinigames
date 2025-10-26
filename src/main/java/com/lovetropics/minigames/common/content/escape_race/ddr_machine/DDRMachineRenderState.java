package com.lovetropics.minigames.common.content.escape_race.ddr_machine;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.AnimationState;
import org.joml.Vector3f;

public class DDRMachineRenderState extends EntityRenderState {
	public float yRot;

	public final AnimationState foldAnimationState = new AnimationState();
	public DDRMachineRenderState() {
		super();
	}
}
