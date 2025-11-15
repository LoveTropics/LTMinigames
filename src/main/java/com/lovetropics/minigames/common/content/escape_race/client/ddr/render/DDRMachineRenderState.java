package com.lovetropics.minigames.common.content.escape_race.client.ddr.render;

import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DDRMachineEntity;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DdrInput;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.TimedDdrInput;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.AnimationState;

import java.util.ArrayList;
import java.util.List;

public class DDRMachineRenderState extends EntityRenderState {
	public float yRot;

	public final AnimationState toBedState = new AnimationState();
	public final AnimationState toDDRState = new AnimationState();

	public List<DDRMachineLevelClientRenderState> levels = new ArrayList<>();
	public DDRMachineEntity.DDRMachineState ddrMachineState;

	public DdrInput input = DdrInput.NONE;

	public boolean isRiding = false;

	public final List<TimedDdrInput> upcomingMoves = new ArrayList<>();

	public long currentTick = 0;

	public DDRMachineRenderState() {
		super();
	}
}
