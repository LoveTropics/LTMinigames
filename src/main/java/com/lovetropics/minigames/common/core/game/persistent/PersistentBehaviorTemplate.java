package com.lovetropics.minigames.common.core.game.persistent;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;

import java.util.function.Supplier;

// Behavior instances are not reusable, so we need to reconstruct from data each time we need them
public sealed interface PersistentBehaviorTemplate {
	Codec<PersistentBehaviorTemplate> CODEC = Codec.PASSTHROUGH.comapFlatMap(
			dynamic -> {
				PersistentBehaviorTemplate template = new Decoding(dynamic);
				return PersistentGameBehavior.CODEC.parse(dynamic).map(behavior -> template);
			},
			template -> new Dynamic<>(JsonOps.INSTANCE, PersistentGameBehavior.CODEC.encodeStart(JsonOps.INSTANCE, template.instantiate())
					.result().orElseThrow())
	);

	PersistentGameBehavior instantiate();

	final class Decoding implements PersistentBehaviorTemplate {
		private final Dynamic<?> data;

		private Decoding(Dynamic<?> data) {
			this.data = data;
		}

		@Override
		public PersistentGameBehavior instantiate() {
			// Data has already been validated, something has gone wrong if we fail to parse again
			return PersistentGameBehavior.CODEC.parse(data).resultOrPartial(s -> {
			}).orElseThrow();
		}
	}

	record Direct(Supplier<PersistentGameBehavior> behavior) implements PersistentBehaviorTemplate {

		@Override
		public PersistentGameBehavior instantiate() {
			return behavior.get();
		}
	}
}
