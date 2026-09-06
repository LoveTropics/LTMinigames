package com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.state;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import org.jspecify.annotations.Nullable;
import java.util.Map;

public final class PlantState {
	private final Map<Key<?>, Object> map = new Reference2ObjectOpenHashMap<>();

	public <S> void put(Key<S> key, S state) {
		map.put(key, state);
	}

	@SuppressWarnings("unchecked")
	public <S> @Nullable S get(Key<S> key) {
		return (S) map.get(key);
	}

	public static final class Key<S> {
		private Key() {
		}

		public static <S> Key<S> create() {
			return new Key<>();
		}
	}
}
