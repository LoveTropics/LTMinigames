package org.lovetropics.games.common.core.game.client_state.instance;

import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateType;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;

public record HideRecipeBookClientState() implements GameClientState {
	public static final HideRecipeBookClientState INSTANCE = new HideRecipeBookClientState();
	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.HIDE_RECIPE_BOOK.get();
	}
}
