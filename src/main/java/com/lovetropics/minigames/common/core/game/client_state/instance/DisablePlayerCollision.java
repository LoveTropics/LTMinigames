package com.lovetropics.minigames.common.core.game.client_state.instance;

import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateType;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;

public class DisablePlayerCollision implements GameClientState {
	public static final DisablePlayerCollision INSTANCE = new DisablePlayerCollision();

	private DisablePlayerCollision() {
	}

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.DISABLE_PLAYER_COLLISION.get();
	}
}
