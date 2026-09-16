package com.lovetropics.minigames.common.core.game.client_state.instance.controls;

import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateType;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;

public record InvertScrollWheelClientState() implements GameClientState {
	public static final InvertScrollWheelClientState INSTANCE = new InvertScrollWheelClientState();

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.INVERT_SCROLL_WHEEL.get();
	}
}
