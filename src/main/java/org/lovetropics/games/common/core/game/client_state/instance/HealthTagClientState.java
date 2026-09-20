package org.lovetropics.games.common.core.game.client_state.instance;

import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateType;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import com.mojang.serialization.MapCodec;

public record HealthTagClientState() implements GameClientState {
	public static final MapCodec<HealthTagClientState> CODEC = MapCodec.unit(HealthTagClientState::new);

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.HEALTH_TAG.get();
	}
}
