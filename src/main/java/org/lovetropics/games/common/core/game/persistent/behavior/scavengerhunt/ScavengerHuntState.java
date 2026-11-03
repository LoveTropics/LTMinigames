package org.lovetropics.games.common.core.game.persistent.behavior.scavengerhunt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class ScavengerHuntState {
	public static final MapCodec<ScavengerHuntState> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.fieldOf("next_stage_index").forGetter(s -> s.nextStageIndex)
	).apply(i, ScavengerHuntState::new));

	private int nextStageIndex;

	private ScavengerHuntState(int nextStageIndex) {
		this.nextStageIndex = nextStageIndex;
	}

	public ScavengerHuntState() {
		this(0);
	}

	public void incrementStageIndex() {
		nextStageIndex++;
	}

	public void setNextStageIndex(int index) {
		nextStageIndex = index;
	}

	public int nextStageIndex() {
		return nextStageIndex;
	}
}
