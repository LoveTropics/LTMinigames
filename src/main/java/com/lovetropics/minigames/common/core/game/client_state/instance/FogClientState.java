package com.lovetropics.minigames.common.core.game.client_state.instance;

import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateType;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record FogClientState(float red, float green, float blue, float nearDistance, float farDistance) implements GameClientState {
	public static final MapCodec<FogClientState> CODEC = RecordCodecBuilder.mapCodec(in -> in.group(
			Codec.floatRange(0.0f, 1.0f).fieldOf("red").forGetter(FogClientState::red),
			Codec.floatRange(0.0f, 1.0f).fieldOf("green").forGetter(FogClientState::green),
			Codec.floatRange(0.0f, 1.0f).fieldOf("blue").forGetter(FogClientState::blue),
			Codec.FLOAT.fieldOf("near_distance").forGetter(FogClientState::nearDistance),
			Codec.FLOAT.fieldOf("far_distance").forGetter(FogClientState::farDistance)
	).apply(in, FogClientState::new));

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.FOG.get();
	}
}
