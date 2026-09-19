package com.lovetropics.minigames.common.core.game.state.progress;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.SharedConstants;

import java.util.function.BooleanSupplier;
import java.util.function.Function;

public sealed interface ProgressionPoint {
	Codec<ProgressionPoint> CODEC = createCodec(Direct.CODEC, Named.CODEC);
	Codec<ProgressionPoint> STRING_CODEC = createCodec(Direct.STRING_CODEC, Named.CODEC);

	private static Codec<ProgressionPoint> createCodec(Codec<Direct> directCodec, Codec<Named> namedCodec) {
		return Codec.either(directCodec, namedCodec).xmap(
				either -> either.map(Function.identity(), Function.identity()),
				point -> switch (point) {
					case Direct direct -> Either.left(direct);
					case Named named -> Either.right(named);
				}
		);
	}

	int UNRESOLVED = -1;

	int resolve(NamedResolver namedResolver);

	default BooleanSupplier createPredicate(IGamePhase game, ProgressChannel channel) {
		ProgressHolder progression = channel.getOrThrow(game);
		return () -> progression.isAfter(this);
	}

	record Direct(float value) implements ProgressionPoint {
		public static final Codec<Direct> CODEC = Codec.FLOAT.xmap(Direct::new, Direct::value);
		public static final Codec<Direct> STRING_CODEC = Codec.STRING.comapFlatMap(string -> {
			try {
				return DataResult.success(new Direct(Float.parseFloat(string)));
			} catch (NumberFormatException e) {
				return DataResult.error(() -> "Not a number: " + string);
			}
		}, direct -> String.valueOf(direct.value()));

		@Override
		public int resolve(NamedResolver namedResolver) {
			return Math.round(value * SharedConstants.TICKS_PER_SECOND);
		}
	}

	record Named(String name) implements ProgressionPoint {
		public static final Codec<Named> CODEC = Codec.STRING.xmap(Named::new, Named::name);

		@Override
		public int resolve(NamedResolver namedResolver) {
			return namedResolver.getNamedPoint(name);
		}
	}

	interface NamedResolver {
		NamedResolver EMPTY = name -> UNRESOLVED;

		int getNamedPoint(String name);
	}
}
