package org.lovetropics.dimensions.duck;

import net.minecraft.core.MappedRegistry;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface RegistryEntryRemover<T> {
	@SuppressWarnings("unchecked")
	static <T> boolean remove(MappedRegistry<T> registry, Identifier key) {
		return ((RegistryEntryRemover<T>) registry).ltdimensions$remove(key);
	}

	@SuppressWarnings("unchecked")
	static <T> boolean remove(MappedRegistry<T> registry, T value) {
		return ((RegistryEntryRemover<T>) registry).ltdimensions$remove(value);
	}

	boolean ltdimensions$remove(T value);

	boolean ltdimensions$remove(Identifier key);
}
