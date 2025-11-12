package com.lovetropics.minigames.common.core.game.behavior;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.instances.CompositeBehavior;
import com.lovetropics.minigames.common.core.game.config.GameConfigs;
import com.lovetropics.minigames.common.core.game.state.GameStateMap;
import com.lovetropics.minigames.common.util.Codecs;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface IGameBehavior {
	Codec<GameBehaviorType<?>> TYPE_CODEC = Codec.either(GameBehaviorTypes.TYPE_CODEC, GameConfigs.CUSTOM_BEHAVIORS)
			.xmap(e -> e.map(Function.identity(), Function.identity()), Either::left);

	Codec<IGameBehavior> SIMPLE_CODEC = Codecs.dispatchWithInlineKey(
			"type",
			TYPE_CODEC,
			behavior -> behavior.behaviorType().get(),
			GameBehaviorType::codec
	);

	// Using custom codec for better error reporting
	Codec<IGameBehavior> CODEC = new Codec<>() {
		@Override
		public <T> DataResult<Pair<IGameBehavior, T>> decode(DynamicOps<T> ops, T input) {
			DataResult<Consumer<Consumer<T>>> list = ops.getList(input);
			if (list.result().isPresent()) {
				return CompositeBehavior.CODEC.decode(ops, input).map(p -> p.mapFirst(b -> b));
			} else {
				return SIMPLE_CODEC.decode(ops, input);
			}
		}

		@Override
		public <T> DataResult<T> encode(IGameBehavior input, DynamicOps<T> ops, T prefix) {
			if (input instanceof CompositeBehavior composite) {
				return CompositeBehavior.CODEC.encode(composite, ops, prefix);
			} else {
				return SIMPLE_CODEC.encode(input, ops, prefix);
			}
		}
	};

	IGameBehavior EMPTY = new CompositeBehavior(List.of());

	default void registerState(IGamePhase game, GameStateMap phaseState, GameStateMap instanceState) {
	}

	/**
	 * Called before the game starts. This should be used to register all event listeners and do any early setup.
	 *
	 * @param game The game that is being constructed
	 * @param events The event listeners to register to
	 * @throws GameException if this behavior was not able to be initialized
	 */
	void register(IGamePhase game, EventRegistrar events) throws GameException;

	// TODO: Implement everywhere
	default Supplier<? extends GameBehaviorType<?>> behaviorType() {
		throw new UnsupportedOperationException("This behavior is not aware of its type: " + this);
	}
}
