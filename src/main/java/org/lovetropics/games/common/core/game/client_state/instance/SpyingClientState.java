package org.lovetropics.games.common.core.game.client_state.instance;

import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateType;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;

public class SpyingClientState implements GameClientState {
	public static final SpyingClientState INSTANCE = new SpyingClientState();

	private SpyingClientState() {}

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.SPYING.get();
	}
}
