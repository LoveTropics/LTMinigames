package com.lovetropics.minigames.common.core.game.state.statistics;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public final class StatisticsMap {
	public static final Codec<StatisticsMap> CODEC = Codec.<StatisticKey<?>, Object>dispatchedMap(StatisticKey.CODEC, StatisticKey::valueCodec).xmap(
			values -> {
				StatisticsMap statistics = new StatisticsMap();
				statistics.values.putAll(values);
				return statistics;
			},
			statistics -> statistics.values
	);

	private final Map<StatisticKey<?>, Object> values = new Reference2ObjectOpenHashMap<>();

	public <T> StatisticsMap set(StatisticKey<T> key, T value) {
		values.put(key, value);
		return this;
	}

	public <T> StatisticsMap setIfAbsent(StatisticKey<T> key, T value) {
		values.putIfAbsent(key, value);
		return this;
	}

	@Nullable
	@SuppressWarnings("unchecked")
	public <T> T get(StatisticKey<T> key) {
		return (T) values.get(key);
	}

	@Contract("_,null->null;_,!null->!null")
	@Nullable
	public <T> T getOr(StatisticKey<T> key, @Nullable T or) {
		T value = get(key);
		return value != null ? value : or;
	}

	public int getInt(StatisticKey<? extends Number> key) {
		Number value = get(key);
		return value != null ? value.intValue() : 0;
	}

	public <T> T getOrElse(StatisticKey<T> key, Supplier<T> orElse) {
		T value = get(key);
		return value != null ? value : orElse.get();
	}

	@Nullable
	@SuppressWarnings("unchecked")
	public <T> T remove(StatisticKey<T> key) {
		return (T) values.remove(key);
	}

	public boolean contains(StatisticKey<?> key) {
		return values.containsKey(key);
	}

	public <T> WithDefault<T> withDefault(StatisticKey<T> key, Supplier<T> defaultSupplier) {
		return new WithDefault<>(key, defaultSupplier);
	}

	public void incrementInt(StatisticKey<Integer> key, int increment) {
		withDefault(key, () -> 0).apply(value -> value + increment);
	}

	public void copyFrom(StatisticsMap fromStatistics, Collection<StatisticKey<?>> keys) {
		for (StatisticKey<?> key : keys) {
			Object value = fromStatistics.values.get(key);
			if (value != null) {
				values.put(key, value);
			}
		}
	}

	public void copyFrom(StatisticsMap fromStatistics) {
		copyFrom(fromStatistics, fromStatistics.values.keySet());
	}

	public class WithDefault<T> {
		private final StatisticKey<T> key;
		private final Supplier<T> defaultSupplier;

		WithDefault(StatisticKey<T> key, Supplier<T> defaultSupplier) {
			this.key = key;
			this.defaultSupplier = defaultSupplier;
		}

		public void apply(UnaryOperator<T> operator) {
			T value = get(key);
			if (value == null) {
				value = defaultSupplier.get();
			}

			value = operator.apply(value);
			set(key, value);
		}

		public void accept(Consumer<T> consumer) {
			T value = get(key);
			if (value == null) {
				value = defaultSupplier.get();
				set(key, value);
			}

			consumer.accept(value);
		}
	}
}
