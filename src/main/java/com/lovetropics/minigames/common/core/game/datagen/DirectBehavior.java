package com.lovetropics.minigames.common.core.game.datagen;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.state.GameStateMap;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public record DirectBehavior(ResourceLocation key, IGameBehavior delegate) implements IGameBehavior {
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
