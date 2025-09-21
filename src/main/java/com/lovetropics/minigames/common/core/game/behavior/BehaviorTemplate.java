package com.lovetropics.minigames.common.core.game.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;

import java.util.function.Supplier;

// Behavior instances are not reusable, so we need to reconstruct from data each time we need them
public sealed interface BehaviorTemplate {
	Codec<BehaviorTemplate> CODEC = Codec.PASSTHROUGH.comapFlatMap(
			dynamic -> {
				BehaviorTemplate template = new Decoding(dynamic);
				return IGameBehavior.CODEC.parse(dynamic).map(behavior -> template);
			},
			template -> new Dynamic<>(JsonOps.INSTANCE, IGameBehavior.CODEC.encodeStart(JsonOps.INSTANCE, template.instantiate())
					.result().orElseThrow())
	);

	IGameBehavior instantiate();

	final class Decoding implements BehaviorTemplate {
		private final Dynamic<?> data;

		private Decoding(Dynamic<?> data) {
			this.data = data;
		}

		@Override
		public IGameBehavior instantiate() {
			// Data has already been validated, something has gone wrong if we fail to parse again
			return IGameBehavior.CODEC.parse(data).resultOrPartial(s -> {
			}).orElseThrow();
		}
	}

	record Direct(Supplier<IGameBehavior> behavior) implements BehaviorTemplate {

		@Override
		public IGameBehavior instantiate() {
			return behavior.get();
		}
	}
}
