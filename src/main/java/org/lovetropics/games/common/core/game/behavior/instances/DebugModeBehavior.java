package org.lovetropics.games.common.core.game.behavior.instances;

import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.state.DebugModeState;
import org.lovetropics.games.common.core.game.state.GameStateMap;
import com.mojang.serialization.MapCodec;

public final class DebugModeBehavior implements IGameBehavior {
	public static final MapCodec<DebugModeBehavior> CODEC = MapCodec.unit(DebugModeBehavior::new);

	@Override
	public void registerState(IGamePhase game, GameStateMap phaseState, GameStateMap instanceState) {
		phaseState.register(DebugModeState.KEY, new DebugModeState());
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
	}
}
