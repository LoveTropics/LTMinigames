package com.lovetropics.minigames.common.content.turtle_race;

import net.minecraft.util.Mth;

import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class RaceTrackPath {
	private final Segment[] segments;
	private final float[] positions;
	private final float length;

	private RaceTrackPath(Segment[] segments, float[] positions, float length) {
		this.segments = segments;
		this.positions = positions;
		this.length = length;
	}

	public static Builder builder() {
		return new Builder();
	}

	public Point nextPointAt(int x, int z, float position, float delta) {
		int closestDistanceSq = Integer.MAX_VALUE;
		Point closestPoint = null;

		float minPosition = position - delta;
		float maxPosition = position + delta;

		int startIndex = findSegment(minPosition);
		for (int i = startIndex; i < segments.length; i++) {
			Segment segment = segments[i];
			if (segment.start().position() > maxPosition) {
				break;
			}

			Point point = segment.closestPointAt(x, z);
			int distanceSq = point.distanceToSq(x, z);
			if (distanceSq < closestDistanceSq) {
				closestDistanceSq = distanceSq;
				closestPoint = point;
			}
		}

		return Objects.requireNonNull(closestPoint);
	}

	public Point closestPointAt(int x, int z) {
		int closestDistanceSq = Integer.MAX_VALUE;
		Point closestPoint = null;
		for (Segment segment : segments) {
			Point point = segment.closestPointAt(x, z);
			int distanceSq = point.distanceToSq(x, z);
			if (distanceSq < closestDistanceSq) {
				closestDistanceSq = distanceSq;
				closestPoint = point;
			}
		}
		return Objects.requireNonNull(closestPoint);
	}

	private int findSegment(float position) {
		int index = Arrays.binarySearch(positions, position);
		if (index >= 0) {
			return index;
		}

		int insertIndex = -index - 1;
		return Math.max(insertIndex - 1, 0);
	}

	public float length() {
		return length;
	}

	private record Segment(Point start, Point end, int deltaX, int deltaZ, int lengthSq) {
		public Point closestPointAt(int x, int z) {
			int distanceAlongSq = distanceAlongSq(x, z);
			if (distanceAlongSq <= 0) {
				return start;
			} else if (distanceAlongSq >= lengthSq) {
				return end;
			}

			float progressAlong = (float) distanceAlongSq / lengthSq;
			return new Point(
					start.x() + Math.floorDiv(deltaX * distanceAlongSq, lengthSq),
					start.z() + Math.floorDiv(deltaZ * distanceAlongSq, lengthSq),
					Mth.lerp(progressAlong, start.position(), end.position())
			);
		}

		public int distanceAlongSq(int x, int z) {
			int relativeX = x - start.x();
			int relativeZ = z - start.z();
			return relativeX * deltaX + relativeZ * deltaZ;
		}
	}

	public record Point(int x, int z, float position) {
		public int distanceToSq(Point point) {
			return distanceToSq(point.x(), point.z());
		}

		public int distanceToSq(int x, int z) {
			return lengthSq(x - this.x, z - this.z);
		}

		private static int lengthSq(int x, int z) {
			return x * x + z * z;
		}
	}

	public static class Builder {
		private final List<Segment> segments = new ArrayList<>();
		private @Nullable Point lastPoint;
		private float length;

		public Builder addPoint(int x, int z) {
			Point point = new Point(x, z, length);
			if (lastPoint != null) {
				length += addSegment(lastPoint, point);
			}
			lastPoint = point;
			return this;
		}

		private float addSegment(Point start, Point end) {
			Segment segment = createSegment(start, end);
			segments.add(segment);
			return Mth.sqrt(segment.lengthSq());
		}

		private static Segment createSegment(Point start, Point end) {
			int deltaX = end.x() - start.x();
			int deltaZ = end.z() - start.z();
			int lengthSq = start.distanceToSq(end);
			return new Segment(start, end, deltaX, deltaZ, lengthSq);
		}

		public Builder loop() {
			if (segments.isEmpty()) {
				throw new IllegalStateException("Must have at least one track segment");
			}
			Point startPoint = segments.getFirst().start();
			return addPoint(startPoint.x(), startPoint.z());
		}

		public RaceTrackPath build() {
			if (lastPoint == null || segments.isEmpty()) {
				throw new IllegalStateException("Must have at least one track segment");
			}

			Segment[] segments = this.segments.toArray(Segment[]::new);
			float[] positions = new float[segments.length];
			for (int i = 0; i < segments.length; i++) {
				positions[i] = segments[i].start().position();
			}

			return new RaceTrackPath(segments, positions, length);
		}
	}
}
