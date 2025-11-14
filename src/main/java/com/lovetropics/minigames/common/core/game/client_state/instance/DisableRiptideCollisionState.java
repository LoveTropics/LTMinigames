package com.lovetropics.minigames.common.core.game.client_state.instance;

import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateType;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;

public class DisableRiptideCollisionState implements GameClientState {
	public static final DisableRiptideCollisionState INSTANCE = new DisableRiptideCollisionState();

	private DisableRiptideCollisionState() {
	}

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.DISABLE_RIPTIDE_COLLISION.get();
	}
}
