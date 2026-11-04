package org.lovetropics.games.common.core.game.state.statistics;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

public interface StatisticsGetter {
	StatisticsGetter EMPTY = new StatisticsGetter() {
		@Override
		public @Nullable <T> T get(StatisticKey<T> key) {
			return null;
		}
	};

	<T> @Nullable T get(StatisticKey<T> key);

	@Contract("_,null->null;_,!null->!null")
	default <T> @Nullable T getOr(StatisticKey<T> key, @Nullable T or) {
		T value = get(key);
		return value != null ? value : or;
	}

	default int getInt(StatisticKey<? extends Number> key) {
		Number value = get(key);
		return value != null ? value.intValue() : 0;
	}

	default <T> T getOrElse(StatisticKey<T> key, Supplier<T> orElse) {
		T value = get(key);
		return value != null ? value : orElse.get();
	}

	default boolean contains(StatisticKey<?> key) {
		return get(key) != null;
	}
}
