package com.lovetropics.minigames.common.content.escape_race.client.ddr;

import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DdrInput;
import net.minecraft.util.BinaryAnimator;
import net.minecraft.util.Mth;

public class DdrPlayerPoseState {
	private static final int ANIMATION_LENGTH = 3;

	private final BinaryAnimator forward = new BinaryAnimator(ANIMATION_LENGTH, Mth::easeInOutSine);
	private final BinaryAnimator back = new BinaryAnimator(ANIMATION_LENGTH, Mth::easeInOutSine);
	private final BinaryAnimator left = new BinaryAnimator(ANIMATION_LENGTH, Mth::easeInOutSine);
	private final BinaryAnimator right = new BinaryAnimator(ANIMATION_LENGTH, Mth::easeInOutSine);

	public void tick(DdrInput input) {
		forward.tick(input.forward());
		back.tick(input.back());
		left.tick(input.left());
		right.tick(input.right());
	}

	public float forward(float partialTicks) {
		return forward.getFactor(partialTicks);
	}

	public float back(float partialTicks) {
		return back.getFactor(partialTicks);
	}

	public float left(float partialTicks) {
		return left.getFactor(partialTicks);
	}

	public float right(float partialTicks) {
		return right.getFactor(partialTicks);
	}
}
