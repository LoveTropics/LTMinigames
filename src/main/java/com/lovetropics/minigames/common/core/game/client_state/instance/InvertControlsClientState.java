package com.lovetropics.minigames.common.core.game.client_state.instance;

import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateType;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record InvertControlsClientState(boolean xAxis, boolean yAxis) implements GameClientState {
	public static final MapCodec<InvertControlsClientState> CODEC = RecordCodecBuilder.mapCodec(in -> in.group(
			Codec.BOOL.optionalFieldOf("x_axis", false).forGetter(InvertControlsClientState::xAxis),
			Codec.BOOL.optionalFieldOf("y_axis", true).forGetter(InvertControlsClientState::yAxis)
	).apply(in, InvertControlsClientState::new));

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.INVERT_CONTROLS.get();
	}
}
