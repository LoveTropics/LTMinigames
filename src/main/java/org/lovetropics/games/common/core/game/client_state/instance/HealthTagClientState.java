package org.lovetropics.games.common.core.game.client_state.instance;

import com.mojang.serialization.MapCodec;
import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateType;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;

public record HealthTagClientState() implements GameClientState {
	public static final MapCodec<HealthTagClientState> CODEC = MapCodec.unit(HealthTagClientState::new);

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.HEALTH_TAG.get();
	}
}
