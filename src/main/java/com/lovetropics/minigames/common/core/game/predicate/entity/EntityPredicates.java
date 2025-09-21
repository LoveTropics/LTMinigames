package com.lovetropics.minigames.common.core.game.predicate.entity;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;
import com.mojang.serialization.MapCodec;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class EntityPredicates {
	public static final ResourceKey<Registry<MapCodec<? extends EntityPredicate>>> REGISTRY_KEY = ResourceKey.createRegistryKey(LoveTropics.location("entity_predicates"));
	public static final DeferredRegister<MapCodec<? extends EntityPredicate>> REGISTER = DeferredRegister.create(REGISTRY_KEY, LoveTropics.ID);

	public static final Registry<MapCodec<? extends EntityPredicate>> REGISTRY = REGISTER.makeRegistry(builder -> builder.sync(false));

	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final RegistryEntry<MapCodec<? extends EntityPredicate>, MapCodec<EntityTypeEntityPredicate>> ENTITY_TYPE = register("entity_type", EntityTypeEntityPredicate.CODEC);

	public static <T extends EntityPredicate> RegistryEntry<MapCodec<? extends EntityPredicate>, MapCodec<T>> register(final String name, final MapCodec<T> codec) {
		return REGISTRATE.object(name).entityPredicate(codec).register();
	}

	public static void init(IEventBus modBus) {
		REGISTER.register(modBus);
	}
}
