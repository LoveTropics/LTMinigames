package org.lovetropics.games.client.game.handler;

import org.lovetropics.games.common.core.game.client_state.GameClientState;

public interface ClientGameStateHandler<T extends GameClientState> {
	void accept(T state);

	void disable(T state);
}
