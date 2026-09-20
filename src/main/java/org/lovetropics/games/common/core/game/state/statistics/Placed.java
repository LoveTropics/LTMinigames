package org.lovetropics.games.common.core.game.state.statistics;

public record Placed<T>(int placement, T value) implements Comparable<Placed<T>> {
	public static <T> Placed<T> at(int placement, T value) {
		return new Placed<>(placement, value);
	}

	@Override
	public int compareTo(Placed<T> other) {
		return Integer.compare(placement, other.placement);
	}
}
