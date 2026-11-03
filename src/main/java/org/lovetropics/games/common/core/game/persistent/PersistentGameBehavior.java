package org.lovetropics.games.common.core.game.persistent;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.persistent.behavior.CompositePersistentBehavior;
import org.lovetropics.games.common.util.Codecs;

import java.util.function.Consumer;
import java.util.function.Function;

public interface PersistentGameBehavior {
	Codec<MapCodec<? extends PersistentGameBehavior>> TYPE_CODEC = Codec.either(PersistentGameBehaviors.TYPE_CODEC, PersistentGameConfigs.CUSTOM_BEHAVIORS)
			.xmap(e -> e.map(Function.identity(), Function.identity()), Either::left);

	Codec<PersistentGameBehavior> SIMPLE_CODEC = Codecs.dispatchWithInlineKey(
			"type",
			TYPE_CODEC,
			PersistentGameBehavior::codec,
			Function.identity()
	);

	// Using custom codec for better error reporting
	Codec<PersistentGameBehavior> CODEC = new Codec<>() {
		@Override
		public <T> DataResult<Pair<PersistentGameBehavior, T>> decode(DynamicOps<T> ops, T input) {
			DataResult<Consumer<Consumer<T>>> list = ops.getList(input);
			if (list.result().isPresent()) {
				return CompositePersistentBehavior.CODEC.decode(ops, input).map(p -> p.mapFirst(b -> b));
			} else {
				return SIMPLE_CODEC.decode(ops, input);
			}
		}

		@Override
		public <T> DataResult<T> encode(PersistentGameBehavior input, DynamicOps<T> ops, T prefix) {
			if (input instanceof CompositePersistentBehavior composite) {
				return CompositePersistentBehavior.CODEC.encode(composite, ops, prefix);
			} else {
				return SIMPLE_CODEC.encode(input, ops, prefix);
			}
		}
	};

	void register(PersistentGame game, EventRegistrar events);

	MapCodec<? extends PersistentGameBehavior> codec();
}
