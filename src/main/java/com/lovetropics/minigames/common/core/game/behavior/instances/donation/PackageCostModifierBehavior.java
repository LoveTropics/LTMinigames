package com.lovetropics.minigames.common.core.game.behavior.instances.donation;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.state.GameStateKey;
import com.lovetropics.minigames.common.core.game.state.GameStateMap;
import com.lovetropics.minigames.common.core.game.state.IGameState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.function.Supplier;

public record PackageCostModifierBehavior(State state) implements IGameBehavior {
	public static final MapCodec<PackageCostModifierBehavior> CODEC = State.CODEC.xmap(PackageCostModifierBehavior::new, PackageCostModifierBehavior::state);

	@Override
	public void registerState(IGamePhase game, GameStateMap phaseState, GameStateMap instanceState) {
		phaseState.register(State.KEY, state);
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.PACKAGE_COST_MODIFIER;
	}

	public record State(double scale, double offset) implements IGameState {
		public static final GameStateKey.Defaulted<State> KEY = GameStateKey.create("Package cost modifier", () -> new State(1.0, 0.0));

		public static final MapCodec<State> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.DOUBLE.optionalFieldOf("scale", 1.0).forGetter(State::scale),
				Codec.DOUBLE.optionalFieldOf("offset", 0.0).forGetter(State::offset)
		).apply(i, State::new));

		public double apply(double amount) {
			return amount * scale + offset;
		}
	}
}
