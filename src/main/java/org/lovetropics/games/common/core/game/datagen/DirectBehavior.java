package org.lovetropics.games.common.core.game.datagen;

import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorType;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.state.GameStateMap;
import net.minecraft.resources.Identifier;

import java.util.function.Supplier;

public record DirectBehavior(Identifier key, IGameBehavior delegate) implements IGameBehavior {
	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return delegate.behaviorType();
	}

	@Override
	public void registerState(IGamePhase game, GameStateMap phaseState, GameStateMap instanceState) {
		delegate.registerState(game, phaseState, instanceState);
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		delegate.register(game, events);
	}
}
