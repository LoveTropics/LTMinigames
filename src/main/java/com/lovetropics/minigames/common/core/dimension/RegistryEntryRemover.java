package com.lovetropics.minigames.common.core.dimension;

import net.minecraft.core.MappedRegistry;
import net.minecraft.resources.Identifier;

public interface RegistryEntryRemover<T> {
	@SuppressWarnings("unchecked")
	static <T> boolean remove(MappedRegistry<T> registry, Identifier key) {
		return ((RegistryEntryRemover<T>) registry).ltminigames$remove(key);
	}

	@SuppressWarnings("unchecked")
	static <T> boolean remove(MappedRegistry<T> registry, T value) {
		return ((RegistryEntryRemover<T>) registry).ltminigames$remove(value);
	}

	boolean ltminigames$remove(T value);

	boolean ltminigames$remove(Identifier key);
}
