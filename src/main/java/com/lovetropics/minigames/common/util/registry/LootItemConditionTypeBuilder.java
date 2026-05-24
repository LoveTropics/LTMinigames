package com.lovetropics.minigames.common.util.registry;

import com.mojang.serialization.MapCodec;
import com.tterrag.registrate.builders.AbstractBuilder;
import com.tterrag.registrate.builders.BuilderCallback;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Supplier;

public final class LootItemConditionTypeBuilder<T extends MapCodec<? extends LootItemCondition>, P> extends AbstractBuilder<MapCodec<? extends LootItemCondition>, T, P, LootItemConditionTypeBuilder<T, P>> {
	private final Supplier<T> condition;

	public LootItemConditionTypeBuilder(LoveTropicsRegistrate owner, P parent, String name, BuilderCallback callback, Supplier<T> condition) {
		super(owner, parent, name, callback, Registries.LOOT_CONDITION_TYPE);
		this.condition = condition;
	}

	@Override
	protected T createEntry() {
		return condition.get();
	}

	@Override
	protected RegistryEntry<MapCodec<? extends LootItemCondition>, T> createEntryWrapper(final DeferredHolder<MapCodec<? extends LootItemCondition>, T> delegate) {
		return new RegistryEntry<>(getOwner(), delegate);
	}
}
