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
			template -> new Dynamic<>(JsonOps.INSTANCE, IGameBehavior.CODEC.encodeStart(JsonOps.INSTANCE, template.instantiate()).getOrThrow())
	);

	IGameBehavior instantiate();

	record Decoding(Dynamic<?> data) implements BehaviorTemplate {
		@Override
		public IGameBehavior instantiate() {
			// Data has already been validated, something has gone wrong if we fail to parse again
			return IGameBehavior.CODEC.parse(data).getPartialOrThrow();
		}
	}

	record Direct(Supplier<IGameBehavior> behavior) implements BehaviorTemplate {
		@Override
		public IGameBehavior instantiate() {
			return behavior.get();
		}
	}
}
