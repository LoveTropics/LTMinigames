package com.lovetropics.minigames.common.util.registry;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// Replicates behavior of dynamic registries - won't be needed once we switch to use those
public class RegistryLoadingOps {
	public static <T> DynamicOps<T> create(DynamicOps<T> ops, HolderLookup.Provider registryAccess) {
		Map<ResourceKey<? extends Registry<?>>, RegistryOps.RegistryInfo<?>> registryInfo = registryAccess.listRegistries()
				.map(registry -> Pair.of(
						registry.key(),
						RegistryOps.RegistryInfo.fromRegistryLookup(registry)
				))
				.collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
		return RegistryOps.create(ops, new RegistryOps.RegistryInfoLookup() {
			@Override
			@SuppressWarnings("unchecked")
			public <V> Optional<RegistryOps.RegistryInfo<V>> lookup(ResourceKey<? extends Registry<? extends V>> key) {
				return Optional.ofNullable((RegistryOps.RegistryInfo<V>) registryInfo.get(key));
			}
		});
	}
}
