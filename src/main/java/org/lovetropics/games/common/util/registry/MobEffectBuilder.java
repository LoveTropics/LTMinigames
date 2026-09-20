package org.lovetropics.games.common.util.registry;

import com.tterrag.registrate.builders.AbstractBuilder;
import com.tterrag.registrate.builders.BuilderCallback;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Supplier;

public final class MobEffectBuilder<T extends MobEffect, P> extends AbstractBuilder<MobEffect, T, P, MobEffectBuilder<T, P>> {
	private final Supplier<T> effect;

	public MobEffectBuilder(LoveTropicsRegistrate owner, P parent, String name, BuilderCallback callback, Supplier<T> effect) {
		super(owner, parent, name, callback, Registries.MOB_EFFECT);
		this.effect = effect;
	}

	public MobEffectBuilder<T, P> lang(String name) {
		return super.lang(MobEffect::getDescriptionId, name);
	}

	@Override
	protected T createEntry() {
		return effect.get();
	}

	@Override
	protected RegistryEntry<MobEffect, T> createEntryWrapper(DeferredHolder<MobEffect, T> delegate) {
		return new RegistryEntry<>(getOwner(), delegate);
	}
}
