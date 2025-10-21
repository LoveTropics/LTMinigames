package com.lovetropics.minigames.common.core.diguise.ability;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Collection;

public final class DisguiseAbilitiesRegistry {
	private static final Multimap<Holder<EntityType<?>>, DisguiseAbilities> REGISTRY = HashMultimap.create();

	private static final Holder<EntityType<?>> GREEN_BASILISK_LIZARD = entityObject(ResourceLocation.fromNamespaceAndPath("tropicraft", "green_basilisk_lizard"));
	private static final Holder<EntityType<?>> BROWN_BASILISK_LIZARD = entityObject(ResourceLocation.fromNamespaceAndPath("tropicraft", "brown_basilisk_lizard"));

	public static void register(EntityType<?> entityType, DisguiseAbilities abilities) {
		register(entityObject(entityType), abilities);
	}

	public static void register(Holder<EntityType<?>> entityType, DisguiseAbilities abilities) {
		REGISTRY.put(entityType, abilities);
	}

	public static DisguiseAbilities create(EntityType<?> entityType) {
		Collection<DisguiseAbilities> abilities = REGISTRY.get(entityObject(entityType));
		return new DisguiseAbilities.Composite(abilities.toArray(new DisguiseAbilities[0]));
	}

	private static Holder<EntityType<?>> entityObject(ResourceLocation entityType) {
		return DeferredHolder.create(Registries.ENTITY_TYPE, entityType);
	}

	private static Holder<EntityType<?>> entityObject(EntityType<?> entityType) {
		return DeferredHolder.create(Registries.ENTITY_TYPE, EntityType.getKey(entityType));
	}
}
