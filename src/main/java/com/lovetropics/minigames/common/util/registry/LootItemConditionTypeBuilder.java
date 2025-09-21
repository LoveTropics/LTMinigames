package com.lovetropics.minigames.common.util.registry;

import com.tterrag.registrate.builders.AbstractBuilder;
import com.tterrag.registrate.builders.BuilderCallback;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonnullType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Supplier;

public final class LootItemConditionTypeBuilder<T extends LootItemConditionType, P> extends AbstractBuilder<LootItemConditionType, T, P, LootItemConditionTypeBuilder<T, P>> {
	private final Supplier<T> condition;

	public LootItemConditionTypeBuilder(LoveTropicsRegistrate owner, P parent, String name, BuilderCallback callback, Supplier<T> condition) {
		super(owner, parent, name, callback, Registries.LOOT_CONDITION_TYPE);
		this.condition = condition;
	}

	@Override
	protected @NonnullType T createEntry() {
		return condition.get();
	}

	@Override
	protected RegistryEntry<LootItemConditionType, T> createEntryWrapper(final DeferredHolder<LootItemConditionType, T> delegate) {
		return new RegistryEntry<>(getOwner(), delegate);
	}
}
