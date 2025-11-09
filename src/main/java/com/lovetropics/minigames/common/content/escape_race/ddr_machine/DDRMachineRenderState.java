package com.lovetropics.minigames.common.content.escape_race.ddr_machine;

import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.DDRMachineLevelClientRenderState;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.TimedDdrInput;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.AnimationState;

import java.util.LinkedList;
import java.util.List;

public class DDRMachineRenderState extends EntityRenderState {
	public float yRot;

	public final AnimationState foldAnimationState = new AnimationState();

	public final List<DDRMachineLevelClientRenderState> levels = new LinkedList<>();
	public DDRMachineEntity.DDRMachineState ddrMachineState;

	public DdrInput input = DdrInput.NONE;

	public boolean isRiding = false;

	public List<TimedDdrInput> upcomingMoves = List.of();

	public int currentTick = 0;

	public DDRMachineRenderState() {
		super();
	}
}
