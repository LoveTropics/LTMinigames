package org.lovetropics.games.common.core.game.client_state.instance;

import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateType;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;

public class BreakDelayState implements GameClientState {
	public static final BreakDelayState INSTANCE = new BreakDelayState();

	private BreakDelayState() {
	}

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.BREAK_DELAY.get();
	}
}
