package org.lovetropics.games.common.core.game.state.progress;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.lovetropics.games.common.core.game.IGamePhase;

import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

public record ProgressionPeriod(ProgressionPoint start, Optional<ProgressionPoint> end) {
	public static final MapCodec<ProgressionPeriod> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ProgressionPoint.CODEC.fieldOf("start").forGetter(ProgressionPeriod::start),
			ProgressionPoint.CODEC.optionalFieldOf("end").forGetter(ProgressionPeriod::end)
	).apply(i, ProgressionPeriod::new));

	public static final Codec<ProgressionPeriod> CODEC = Codec.either(MAP_CODEC.codec(), ProgressionPoint.CODEC.listOf(2, 2)).xmap(
			either -> either.map(Function.identity(), points -> new ProgressionPeriod(points.getFirst(), Optional.of(points.getLast()))),
			Either::left
	);

	public BooleanSupplier createPredicate(IGamePhase game, ProgressChannel channel) {
		ProgressHolder holder = channel.getOrThrow(game);
		return () -> holder.is(this);
	}
}
