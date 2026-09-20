package org.lovetropics.games.common.core.game.behavior.instances;

import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.state.GameStateMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import java.util.List;

public record CompositeBehavior(List<IGameBehavior> behaviors) implements IGameBehavior {
	public static final Codec<CompositeBehavior> CODEC = IGameBehavior.CODEC.listOf().xmap(CompositeBehavior::new, CompositeBehavior::behaviors);
	public static final MapCodec<CompositeBehavior> MAP_CODEC = CODEC.fieldOf("behaviors");

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		for (IGameBehavior behavior : behaviors) {
			behavior.register(game, events);
		}
	}

	@Override
	public void registerState(IGamePhase game, GameStateMap phaseState, GameStateMap instanceState) {
		for (IGameBehavior behavior : behaviors) {
			behavior.registerState(game, phaseState, instanceState);
		}
	}
}
