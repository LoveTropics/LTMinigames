package org.lovetropics.games.common.core.game.behavior.instances.donation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.state.GameStateKey;
import org.lovetropics.games.common.core.game.state.GameStateMap;
import org.lovetropics.games.common.core.game.state.IGameState;

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
	public MapCodec<? extends IGameBehavior> codec() {
		return CODEC;
	}

	public record State(double scale, double offset) implements IGameState {
		public static final State NONE = new State(1.0, 0.0);
		public static final GameStateKey.Defaulted<State> KEY = GameStateKey.create("Package cost modifier", () -> NONE);

		public static final MapCodec<State> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.DOUBLE.optionalFieldOf("scale", 1.0).forGetter(State::scale),
				Codec.DOUBLE.optionalFieldOf("offset", 0.0).forGetter(State::offset)
		).apply(i, State::new));

		public double apply(double amount) {
			return amount * scale + offset;
		}
	}
}
