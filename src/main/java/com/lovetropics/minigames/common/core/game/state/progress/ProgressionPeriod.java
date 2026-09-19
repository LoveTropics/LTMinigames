package com.lovetropics.minigames.common.core.game.state.progress;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.function.BooleanSupplier;

public record ProgressionPeriod(ProgressionPoint start, ProgressionPoint end) {
	public static final MapCodec<ProgressionPeriod> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ProgressionPoint.CODEC.fieldOf("start").forGetter(ProgressionPeriod::start),
			ProgressionPoint.CODEC.fieldOf("end").forGetter(ProgressionPeriod::end)
	).apply(i, ProgressionPeriod::new));

	private static final Codec<ProgressionPeriod> TUPLE_CODEC = ProgressionPoint.CODEC.listOf(2, 2).xmap(
			points -> new ProgressionPeriod(points.getFirst(), points.getLast()),
			period -> List.of(period.start, period.end)
	);

	public static final Codec<ProgressionPeriod> CODEC = Codec.withAlternative(MAP_CODEC.codec(), TUPLE_CODEC);

	public BooleanSupplier createPredicate(IGamePhase game, ProgressChannel channel) {
		ProgressHolder holder = channel.getOrThrow(game);
		return () -> holder.is(this);
	}
}
