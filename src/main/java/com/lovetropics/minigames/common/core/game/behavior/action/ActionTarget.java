package com.lovetropics.minigames.common.core.game.behavior.action;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameEventListeners;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import org.apache.commons.lang3.function.ToBooleanBiFunction;

import java.util.List;
import java.util.function.Function;

public interface ActionTarget<T> {
	Codec<ActionTarget<?>> CODEC = Codec.lazyInitialized(() -> ActionTargetTypes.REGISTRY.byNameCodec())
			.dispatch(ActionTarget::type, codec -> {
				if (codec instanceof MapCodec.MapCodecCodec<? extends ActionTarget<?>> mapCodecCodec) {
					return mapCodecCodec.codec();
				}
				return codec.fieldOf("value");
			});
	Codec<ActionTarget<?>> FALLBACK_PLAYER = Codec.xor(CODEC, PlayerActionTarget.Target.CODEC)
			.xmap(e -> e.map(Function.identity(), PlayerActionTarget::new), Either::left);

	List<T> resolve(IGamePhase phase, Iterable<T> sources);

	boolean apply(IGamePhase game, GameEventListeners listeners, GameActionContext context, Iterable<T> sources);

	void listenAndCaptureSource(EventRegistrar listeners, ToBooleanBiFunction<GameActionContext, Iterable<T>> listener);

	default boolean requiresSource() {
		return false;
	}

	Codec<? extends ActionTarget<T>> type();
}
