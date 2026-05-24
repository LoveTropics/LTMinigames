package com.lovetropics.minigames.common.util.registry;

import com.tterrag.registrate.builders.AbstractBuilder;
import com.tterrag.registrate.builders.BuilderCallback;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Function;

public final class AttributeBuilder<T extends Attribute, P> extends AbstractBuilder<Attribute, T, P, AttributeBuilder<T, P>> {
	private final Function<String, T> factory;

	public AttributeBuilder(final LoveTropicsRegistrate owner, final P parent, final String name, final BuilderCallback callback, final Function<String, T> factory) {
		super(owner, parent, name, callback, Registries.ATTRIBUTE);
		this.factory = factory;
	}

	public AttributeBuilder<T, P> lang(final String name) {
		return super.lang(Attribute::getDescriptionId, name);
	}

	@Override
	protected T createEntry() {
		final Identifier id = Identifier.fromNamespaceAndPath(getOwner().getModid(), getName());
		final String translationKey = Util.makeDescriptionId("attribute", id);
		return factory.apply(translationKey);
	}

	@Override
	protected RegistryEntry<Attribute, T> createEntryWrapper(final DeferredHolder<Attribute, T> delegate) {
		return new RegistryEntry<>(getOwner(), delegate);
	}
}
