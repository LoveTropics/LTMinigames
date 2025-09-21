package com.lovetropics.minigames.common.core.game.behavior.config;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ConfigList {
	private final ResourceLocation id;
	private final Map<BehaviorConfig<?>, ConfigData> configs;

	public ConfigList(ResourceLocation id, Map<BehaviorConfig<?>, ConfigData> configs) {
		this.id = id;
		this.configs = configs;
	}

	public ConfigData getData(BehaviorConfig<?> key) {
		return configs.get(key);
	}

	public Set<BehaviorConfig<?>> keySet() {
		return Collections.unmodifiableSet(configs.keySet());
	}

	public <T> DataResult<Pair<T, ConfigData>> decode(BehaviorConfig<T> key) {
		return key.getCodec().decode(ConfigDataOps.INSTANCE, getData(key));
	}

	@Override
	public String toString() {
		return "ConfigList" + id + ":" + configs;
	}

	public static Builder builder(ResourceLocation id) {
		return new Builder(id);
	}

	public ResourceLocation id() {
		return id;
	}

	public static class Builder {
		private final ResourceLocation id;
		private final Map<BehaviorConfig<?>, ConfigData> configs = new LinkedHashMap<>();

		Builder(ResourceLocation id) {
			this.id = id;
		}

		public <T> Builder with(BehaviorConfig<T> key, T defVal) {
			ConfigData data = key.getCodec().encodeStart(ConfigDataOps.INSTANCE, defVal).getOrThrow();
			configs.put(key, data);
			return this;
		}

		public ConfigList build() {
			return new ConfigList(id, configs);
		}
	}
}
