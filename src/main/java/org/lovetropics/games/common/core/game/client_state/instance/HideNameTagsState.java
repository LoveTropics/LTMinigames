package org.lovetropics.games.common.core.game.client_state.instance;

import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateType;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;

public class HideNameTagsState implements GameClientState {
	public static final HideNameTagsState INSTANCE = new HideNameTagsState();

	private HideNameTagsState() {
	}

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.HIDE_NAME_TAGS.get();
	}
}
