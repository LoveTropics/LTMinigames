package com.lovetropics.minigames.common.content.escape_race.ddr_machine;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.AnimationState;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DDRMachineRenderState extends EntityRenderState {
	public float yRot;

	public final AnimationState foldAnimationState = new AnimationState();

	public final List<DDRMachineLevelClientRenderState> levels = new LinkedList<>();
	public DDRMachineEntity.DDRMachineState ddrMachineState;

	public boolean leftPressed = false;
	public boolean upPressed = false;
	public boolean downPressed = false;
	public boolean rightPressed = false;

	public final Map<Integer, DDRMachineLevelTick> upcomingMoves = new Int2ObjectOpenHashMap<>();

	public int currentTick = 0;

	public DDRMachineRenderState() {
		super();
	}
}
