package org.lovetropics.games.common.core.game.client_state.instance.controls;

import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateType;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;

public record ReverseRiptideClientState() implements GameClientState {
	public static final ReverseRiptideClientState INSTANCE = new ReverseRiptideClientState();

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.REVERSE_RIPTIDE.get();
	}
}
