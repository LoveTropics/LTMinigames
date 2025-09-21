package com.lovetropics.minigames.common.core.game.client_state.instance;

import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateType;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.mojang.serialization.MapCodec;

public record SwapMovementClientState() implements GameClientState {
	public static final MapCodec<SwapMovementClientState> CODEC = MapCodec.unit(SwapMovementClientState::new);

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.SWAP_MOVEMENT.get();
	}
}
