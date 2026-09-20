package org.lovetropics.games.common.util;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import net.minecraft.util.ExtraCodecs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class LinearSpline implements Float2FloatFunction {
	private static final Codec<LinearSpline> FULL_CODEC = ExtraCodecs.nonEmptyList(Point.CODEC.listOf()).xmap(
			points -> {
				Builder spline = builder();
				spline.points.addAll(points);
				return spline.build();
			},
			LinearSpline::points
	);

	public static final Codec<LinearSpline> CODEC = Codec.withAlternative(
			Codec.withAlternative(
					FULL_CODEC,
					Codec.mapPair(
							Codec.FLOAT.fieldOf("start"),
							Codec.FLOAT.fieldOf("end")
					).codec(),
					pair -> builder().point(0.0f, pair.getFirst()).point(1.0f, pair.getSecond()).build()
			),
			Codec.FLOAT,
			LinearSpline::constant
	);

	private final float[] xs;
	private final float[] ys;
	private final float[] gradients;

	private LinearSpline(float[] xs, float[] ys, float[] gradients) {
		this.xs = xs;
		this.ys = ys;
		this.gradients = gradients;
	}

	public static LinearSpline constant(float y) {
		return new LinearSpline(new float[1], new float[]{y}, new float[1]);
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public float get(float x) {
		int index = indexAt(x);
		if (index <= 0) {
			return ys[0];
		} else if (index >= ys.length) {
			return ys[ys.length - 1];
		}

		float x0 = xs[index];
		float y0 = ys[index];
		float gradient = gradients[index - 1];
		return (x - x0) * gradient + y0;
	}

	private int indexAt(float x) {
		int i = Arrays.binarySearch(xs, x);
		return i >= 0 ? i : -i - 1;
	}

	private List<Point> points() {
		List<Point> points = new ArrayList<>(xs.length);
		for (int i = 0; i < xs.length; i++) {
			points.add(new Point(xs[i], ys[i]));
		}
		return points;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof LinearSpline spline) {
			return Arrays.equals(xs, spline.xs) && Arrays.equals(ys, spline.ys);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(xs) * 31 + Arrays.hashCode(ys);
	}

	public static class Builder {
		private final List<Point> points = new ArrayList<>();

		private Builder() {
		}

		public Builder point(float x, float y) {
			points.add(new Point(x, y));
			return this;
		}

		public LinearSpline build() {
			if (points.isEmpty()) {
				throw new IllegalStateException("Cannot have less than 1 point");
			}

			float[] xs = new float[points.size()];
			float[] ys = new float[points.size()];
			float[] gradients = new float[points.size() - 1];

			points.sort(Comparator.comparingDouble(Point::x));

			for (int i = 0; i < points.size(); i++) {
				Point point = points.get(i);
				xs[i] = point.x();
				ys[i] = point.y();
			}

			for (int i = 0; i < points.size() - 1; i++) {
				Point start = points.get(i);
				Point end = points.get(i + 1);
				gradients[i] = (end.y() - start.y()) / (end.x() - start.x());
			}

			return new LinearSpline(xs, ys, gradients);
		}
	}

	private record Point(float x, float y) {
		public static final Codec<Point> CODEC = Codec.FLOAT.listOf(2, 2).xmap(
				floats -> new Point(floats.getFirst(), floats.getLast()),
				point -> List.of(point.x, point.y)
		);
	}
}
