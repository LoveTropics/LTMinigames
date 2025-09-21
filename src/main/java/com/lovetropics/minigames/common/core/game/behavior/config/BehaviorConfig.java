package com.lovetropics.minigames.common.core.game.behavior.config;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.lovetropics.minigames.common.core.game.behavior.config.ConfigData.CompositeConfigData;
import com.lovetropics.minigames.common.core.game.behavior.config.ConfigData.ListConfigData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public final class BehaviorConfig<A> extends MapCodec<A> {

	public static final Map<String, BehaviorConfig<?>> TEMP_REGISTRY = new HashMap<>();

	private final String name;
	private final Multimap<String, UnaryOperator<ConfigData>> hints = HashMultimap.create();
	private final Codec<A> codec;

	private BehaviorConfig(String name, Codec<A> codec) {
		this.name = name;
		this.codec = codec;
		TEMP_REGISTRY.put(name, this);
	}

	public static <A> BehaviorConfig<A> fieldOf(String name, Codec<A> codec) {
		return new BehaviorConfig<>(name, codec);
	}

	public <E extends Enum<E>> BehaviorConfig<A> enumHint(String key, Function<String, E> fromName) {
		return hint(key, data -> new ConfigData.SimpleConfigData(ConfigType.ENUM, fromName.apply((String) data.value())));
	}

	public BehaviorConfig<A> listTypeHint(String key, ConfigType type) {
		return hint(key, data -> ((ConfigData.ListConfigData) data).setComponentType(type));
	}

	public <B> BehaviorConfig<A> defaultValueHint(String key, B instance) {
		return hint(key, data -> ((ConfigData.ListConfigData) data).setDefaultValue(ConfigType.COMPOSITE).setDefaultValue(instance));
	}

	public <B> BehaviorConfig<A> defaultInstanceHint(String key, B instance, Codec<B> codec) {
		return hint(key, data -> ((ConfigData.ListConfigData) data).setDefaultValue(codec.encodeStart(ConfigDataOps.INSTANCE, instance).getOrThrow()));
	}

	private BehaviorConfig<A> hint(String key, UnaryOperator<ConfigData> hint) {
		hints.put(key, hint);
		return this;
	}

	public ConfigData postProcess(ConfigData data) {
		return postProcessRecursive("", data);
	}

	private ConfigData postProcessRecursive(String path, ConfigData data) {
		if (data instanceof CompositeConfigData composite) {
			for (String key : composite.keys()) {
				composite.put(key, postProcessRecursive(path.isEmpty() ? key : path + "." + key, composite.value(key)));
			}
		} else if (data instanceof ListConfigData list) {
			if (list.componentType() == ConfigType.COMPOSITE) {
				List<Object> values = list.value();
				for (int i = 0; i < values.size(); i++) {
					values.set(i, postProcessRecursive(path.isEmpty() ? "[]" : path + ".[]", (ConfigData) values.get(i)));
				}
			}
		}

		Collection<UnaryOperator<ConfigData>> hints = this.hints.get(path);
		for (UnaryOperator<ConfigData> hint : hints) {
			data = hint.apply(data);
		}
		return data;
	}

	public A getValue(ConfigList configs) {
		return configs.decode(this).getOrThrow().getFirst();
	}

	public String getName() {
		return name;
	}

	public Codec<A> getCodec() {
		return codec;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public <T> Stream<T> keys(DynamicOps<T> ops) {
		return Stream.of(ops.createString(name));
	}

	@Override
	public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
		return codec.parse(ops, input.get(name));
	}

	@Override
	public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
		DataResult<T> fieldResult = codec.encodeStart(ops, input);
		return prefix.add(name, fieldResult);
	}
}
