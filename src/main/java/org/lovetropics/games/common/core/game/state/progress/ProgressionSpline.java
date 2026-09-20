package org.lovetropics.games.common.core.game.state.progress;

import org.lovetropics.games.common.util.LinearSpline;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.function.DoubleSupplier;

public class ProgressionSpline {
	public static final Codec<ProgressionSpline> CODEC = Entry.CODEC.listOf().xmap(ProgressionSpline::new, s -> s.entries);

	private final List<Entry> entries;

	public ProgressionSpline(List<Entry> entries) {
		this.entries = entries;
	}

	public DoubleSupplier resolve(ProgressHolder progression) {
		LinearSpline.Builder spline = LinearSpline.builder();
		for (Entry entry : entries) {
			int point = entry.point().resolve(progression);
			if (point == ProgressionPoint.UNRESOLVED) {
				throw new IllegalStateException("Could not resolve point: " + entry.point);
			}
			float value = entry.value();
			spline.point(point, value);
		}
		LinearSpline builtSpline = spline.build();
		return () -> builtSpline.get(progression.time());
	}

	public record Entry(ProgressionPoint point, float value) {
		public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
				ProgressionPoint.CODEC.fieldOf("point").forGetter(Entry::point),
				Codec.FLOAT.fieldOf("value").forGetter(Entry::value)
		).apply(i, Entry::new));
	}
}
