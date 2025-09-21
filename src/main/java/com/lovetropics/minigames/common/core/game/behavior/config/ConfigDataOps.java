package com.lovetropics.minigames.common.core.game.behavior.config;

import com.google.common.collect.Lists;
import com.lovetropics.minigames.common.core.game.behavior.config.ConfigData.CompositeConfigData;
import com.lovetropics.minigames.common.core.game.behavior.config.ConfigData.ListConfigData;
import com.lovetropics.minigames.common.core.game.behavior.config.ConfigData.SimpleConfigData;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.ListBuilder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class ConfigDataOps implements DynamicOps<ConfigData> {
	public static final ConfigDataOps INSTANCE = new ConfigDataOps();

	protected ConfigDataOps() {
	}

	@Override
	public ConfigData empty() {
		return ListConfigData.EMPTY; // TODO
	}

	@Override
	public <U> U convertTo(final DynamicOps<U> outOps, final ConfigData input) {
		if (input instanceof CompositeConfigData) {
			return convertMap(outOps, input);
		}
		if (input instanceof ListConfigData) {
			return convertList(outOps, input);
		}
		final SimpleConfigData primitive = (SimpleConfigData) input;
		return switch (primitive.type()) {
			case STRING -> outOps.createString(primitive.value().toString());
			case NUMBER -> outOps.createNumeric((Number) primitive.value());
			case BOOLEAN -> outOps.createBoolean((Boolean) primitive.value());
			case ENUM -> outOps.createString(((Enum<?>) primitive.value()).name());
			case LIST, COMPOSITE ->
					throw new IllegalStateException("Cannot serialize list/composite config as a primitive");
			default -> throw new IllegalArgumentException("Unknown config type: " + primitive.type());
		};
	}

	@Override
	public DataResult<Number> getNumberValue(final ConfigData input) {
		if (input.type() == ConfigType.NUMBER) {
			return DataResult.success((Number) input.value());
		}
		return DataResult.error(() -> "Not a number: " + input.value());
	}

	@Override
	public ConfigData createByte(byte value) {
		if (value == 0 || value == 1) {
			// NBT coalesces booleans to bytes.
			// This reverses that, at the cost of some ambiguity, however it is very
			// unlikely for a true byte value to come through this code
			ConfigData ret = new SimpleConfigData(ConfigType.BOOLEAN);
			ret.setValue(value == 1);
			return ret;
		}
		return DynamicOps.super.createByte(value);
	}

	@Override
	public ConfigData createNumeric(final Number i) {
		ConfigData ret = new SimpleConfigData(ConfigType.NUMBER);
		ret.setValue(i);
		return ret;
	}

	@Override
	public DataResult<Boolean> getBooleanValue(final ConfigData input) {
		if (input.type() == ConfigType.BOOLEAN) {
			return DataResult.success((Boolean) input.value());
		}
		return DataResult.error(() -> "Not a boolean: " + input.value());
	}

	@Override
	public ConfigData createBoolean(final boolean value) {
		ConfigData ret = new SimpleConfigData(ConfigType.BOOLEAN);
		ret.setValue(value);
		return ret;
	}

	@Override
	public DataResult<String> getStringValue(final ConfigData input) {
		if (input.type() == ConfigType.STRING) {
			return DataResult.success((String) input.value());
		}
		return DataResult.error(() -> "Not a string: " + input.value());
	}

	@Override
	public ConfigData createString(final String value) {
		ConfigData ret = new SimpleConfigData(ConfigType.STRING);
		ret.setValue(value);
		return ret;
	}

	@Override
	public DataResult<ConfigData> mergeToList(final ConfigData list, final ConfigData value) {
		if (list.type() != ConfigType.LIST && list != empty()) {
			return DataResult.error(() -> "mergeToList called with not a list: " + list, list);
		}
		if (((ListConfigData) list).componentType() != value.type()) {
			return DataResult.error(() -> "mergeToList called with value that is invalid for given list: " + value, list);
		}

		final ListConfigData result = new ListConfigData(((ListConfigData) list).componentType());
		if (list != empty()) {
			result.addAll((Collection<?>) list.value());
		}
		result.add(value.value());
		return DataResult.success(result);
	}

	@Override
	public DataResult<ConfigData> mergeToList(final ConfigData list, final List<ConfigData> values) {
		if (list.type() != ConfigType.LIST && list != empty()) {
			return DataResult.error(() -> "mergeToList called with not a list: " + list, list);
		}
		if (values.stream().map(ConfigData::type).allMatch(((ListConfigData) list).componentType()::equals)) {
			return DataResult.error(() -> "mergeToList called with value that is invalid for given list: " + values, list);
		}

		final ListConfigData result = new ListConfigData(((ListConfigData) list).componentType());
		if (list != empty()) {
			result.addAll((Collection<?>) list.value());
		}
		values.stream().map(ConfigData::value).forEach(result::add);
		return DataResult.success(result);
	}

	@Override
	public DataResult<ConfigData> mergeToMap(final ConfigData map, final ConfigData key, final ConfigData value) {
		if (map.type() != ConfigType.COMPOSITE && map != empty()) {
			return DataResult.error(() -> "mergeToMap called with not a map: " + map, map);
		}
		if (key.type() != ConfigType.STRING) {
			return DataResult.error(() -> "key is not a string: " + key, map);
		}

		final CompositeConfigData output = new CompositeConfigData();
		if (map != empty()) {
			((CompositeConfigData) map).value().forEach(output::addChild);
		}
		output.addChild((String) key.value(), value);

		return DataResult.success(output);
	}

	@Override
	public DataResult<ConfigData> mergeToMap(final ConfigData map, final MapLike<ConfigData> values) {
		if (map.type() != ConfigType.COMPOSITE && map != empty()) {
			return DataResult.error(() -> "mergeToMap called with not a map: " + map, map);
		}

		final CompositeConfigData output = new CompositeConfigData();
		if (map != empty()) {
			((CompositeConfigData) map).value().forEach(output::addChild);
		}

		final List<ConfigData> missed = Lists.newArrayList();

		values.entries().forEach(entry -> {
			final ConfigData key = entry.getFirst();
			if (key.type() != ConfigType.STRING) {
				missed.add(key);
				return;
			}
			output.addChild((String) key.value(), entry.getSecond());
		});

		if (!missed.isEmpty()) {
			return DataResult.error(() -> "some keys are not strings: " + missed, output);
		}

		return DataResult.success(output);
	}

	@Override
	public DataResult<Stream<Pair<ConfigData, ConfigData>>> getMapValues(final ConfigData input) {
		if (input.type() != ConfigType.COMPOSITE) {
			return DataResult.error(() -> "Not a composite config: " + input);
		}
		return DataResult.success(((CompositeConfigData) input).value().entrySet().stream().map(entry -> Pair.of(new SimpleConfigData(ConfigType.STRING, entry.getKey()), entry.getValue())));
	}

	@Override
	public DataResult<Consumer<BiConsumer<ConfigData, ConfigData>>> getMapEntries(final ConfigData input) {
		if (input.type() != ConfigType.COMPOSITE) {
			return DataResult.error(() -> "Not a composite config: " + input);
		}
		return DataResult.success(c -> {
			for (final Map.Entry<String, ConfigData> entry : ((CompositeConfigData) input).value().entrySet()) {
				c.accept(createString(entry.getKey()), entry.getValue());
			}
		});
	}

	@Override
	public DataResult<MapLike<ConfigData>> getMap(final ConfigData input) {
		if (input.type() != ConfigType.COMPOSITE) {
			return DataResult.error(() -> "Not a composite config: " + input);
		}
		final CompositeConfigData composite = (CompositeConfigData) input;
		return DataResult.success(new MapLike<>() {
			@Nullable
			@Override
			public ConfigData get(final ConfigData key) {
				return composite.value(key.value().toString());
			}

			@Nullable
			@Override
			public ConfigData get(final String key) {
				return composite.value(key);
			}

			@Override
			public Stream<Pair<ConfigData, ConfigData>> entries() {
				return composite.value().entrySet().stream().map(e -> Pair.of(new SimpleConfigData(ConfigType.STRING, e.getKey()), e.getValue()));
			}

			@Override
			public String toString() {
				return "MapLike[" + composite + "]";
			}
		});
	}

	@Override
	public ConfigData createMap(final Stream<Pair<ConfigData, ConfigData>> map) {
		final CompositeConfigData result = new CompositeConfigData();
		map.forEach(p -> result.addChild(p.getFirst().value().toString(), p.getSecond()));
		return result;
	}

	@Override
	public DataResult<Stream<ConfigData>> getStream(final ConfigData input) {
		if (input.type() == ConfigType.LIST) {
			ListConfigData list = (ListConfigData) input;
			return DataResult.success(list.value().stream().map(v -> {
				if (list.componentType().isComplex()) {
					return (ConfigData) v;
				} else {
					return new SimpleConfigData(list.componentType(), v);
				}
			}));
		}
		return DataResult.error(() -> "Not a list config: " + input);
	}

	@Override
	public DataResult<Consumer<Consumer<ConfigData>>> getList(final ConfigData input) {
		if (input.type() == ConfigType.LIST) {
			ListConfigData list = (ListConfigData) input;
			return DataResult.success(c -> {
				for (final Object element : list.value()) {
					if (list.componentType().isComplex()) {
						c.accept((ConfigData) element);
					} else {
						c.accept(new SimpleConfigData(list.componentType(), element));
					}
				}
			});
		}
		return DataResult.error(() -> "Not a list config: " + input);
	}

	@Override
	public ConfigData createList(final Stream<ConfigData> input) {
		List<ConfigData> list = input.toList();
		if (list.stream().map(ConfigData::type).distinct().count() > 1) {
			throw new IllegalArgumentException("List config cannot contain heterogenous values: " + input);
		}
		if (list.isEmpty()) {
			return ListConfigData.EMPTY;
		}
		final ListConfigData result = new ListConfigData(list.getFirst().type());
		list.stream().map(d -> d.type().isComplex() ? d : d.value()).forEach(result::add);
		return result;
	}

	@Override
	public ConfigData remove(final ConfigData input, final String key) {
		if (input.type() == ConfigType.COMPOSITE) {
			final CompositeConfigData result = new CompositeConfigData();
			((CompositeConfigData) input).value().entrySet().stream().filter(entry -> !Objects.equals(entry.getKey(), key)).forEach(entry -> result.addChild(entry.getKey(), entry.getValue()));
			return result;
		}
		return input;
	}

	@Override
	public String toString() {
		return "LTMinigames Config Data";
	}

	@Override
	public ListBuilder<ConfigData> listBuilder() {
		return new ArrayBuilder();
	}

	private final class ArrayBuilder implements ListBuilder<ConfigData> {
		private DataResult<ListConfigData> builder = DataResult.success(ListConfigData.EMPTY, Lifecycle.stable());

		@Override
		public DynamicOps<ConfigData> ops() {
			return INSTANCE;
		}

		@Override
		public ListBuilder<ConfigData> add(final ConfigData value) {
			builder = builder.map(b -> {
				if (b == ListConfigData.EMPTY) {
					b = new ListConfigData(value.type());
				}
				b.add(value.value());
				return b;
			});
			return this;
		}

		@Override
		public ListBuilder<ConfigData> add(final DataResult<ConfigData> value) {
			builder = builder.apply2stable((b, element) -> {
				if (b == ListConfigData.EMPTY) {
					b = new ListConfigData(element.type());
				}
				b.add(element.type() == ConfigType.COMPOSITE ? element : element.value());
				return b;
			}, value);
			return this;
		}

		@Override
		public ListBuilder<ConfigData> withErrorsFrom(final DataResult<?> result) {
			builder = builder.flatMap(r -> result.map(v -> r));
			return this;
		}

		@Override
		public ListBuilder<ConfigData> mapError(final UnaryOperator<String> onError) {
			builder = builder.mapError(onError);
			return this;
		}

		@Override
		public DataResult<ConfigData> build(final ConfigData prefix) {
			final DataResult<ConfigData> result = builder.flatMap(b -> {
				if (prefix == empty()) {
					return DataResult.success(b);
				}
				if (prefix.type() != ConfigType.LIST) {
					return DataResult.error(() -> "Cannot append a list to not a list: " + prefix, prefix);
				}
				ListConfigData prefixList = (ListConfigData) prefix;
				if (prefixList == ListConfigData.EMPTY) {
					return DataResult.success(b);
				}
				if (prefixList.componentType() != b.componentType() && b != ListConfigData.EMPTY) {
					return DataResult.error(() -> "Cannot combine lists of different component types: " + prefixList.componentType(), prefix);
				}

				final ListConfigData res = new ListConfigData(prefixList.componentType());
				if (prefixList != ListConfigData.EMPTY) {
					res.addAll(prefixList.value());
				}
				res.addAll(b.value());
				return DataResult.success(res, Lifecycle.stable());
			});

			builder = DataResult.success(ListConfigData.EMPTY, Lifecycle.stable());
			return result;
		}
	}

	@Override
	public boolean compressMaps() {
		return false;
	}

	@Override
	public RecordBuilder<ConfigData> mapBuilder() {
		return new ConfigDataRecordBuilder();
	}

	private class ConfigDataRecordBuilder extends RecordBuilder.AbstractStringBuilder<ConfigData, CompositeConfigData> {
		protected ConfigDataRecordBuilder() {
			super(ConfigDataOps.this);
		}

		@Override
		protected CompositeConfigData initBuilder() {
			return new CompositeConfigData();
		}

		@Override
		protected CompositeConfigData append(final String key, final ConfigData value, final CompositeConfigData builder) {
			builder.addChild(key, value);
			return builder;
		}

		@Override
		protected DataResult<ConfigData> build(final CompositeConfigData builder, final ConfigData prefix) {
			if (prefix == null || prefix == empty()) {
				return DataResult.success(builder);
			}
			if (prefix.type() == ConfigType.COMPOSITE) {
				final CompositeConfigData result = new CompositeConfigData();
				for (final Map.Entry<String, ConfigData> entry : ((CompositeConfigData) prefix).value().entrySet()) {
					result.addChild(entry.getKey(), entry.getValue());
				}
				for (final Map.Entry<String, ConfigData> entry : builder.value().entrySet()) {
					result.addChild(entry.getKey(), entry.getValue());
				}
				return DataResult.success(result);
			}
			return DataResult.error(() -> "mergeToMap called with not a map: " + prefix, prefix);
		}
	}
}
