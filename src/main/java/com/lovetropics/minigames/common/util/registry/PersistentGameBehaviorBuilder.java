package com.lovetropics.minigames.common.util.registry;

import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehavior;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehaviorEntry;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehaviorType;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehaviors;
import com.mojang.serialization.MapCodec;
import com.tterrag.registrate.builders.AbstractBuilder;
import com.tterrag.registrate.builders.BuilderCallback;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class PersistentGameBehaviorBuilder<T extends PersistentGameBehavior, P> extends AbstractBuilder<PersistentGameBehaviorType<?>, PersistentGameBehaviorType<T>, P, PersistentGameBehaviorBuilder<T, P>> {
	private final MapCodec<T> codec;

	public PersistentGameBehaviorBuilder(LoveTropicsRegistrate owner, P parent, String name, BuilderCallback callback, MapCodec<T> codec) {
		super(owner, parent, name, callback, PersistentGameBehaviors.REGISTRY_KEY);
		this.codec = codec;
	}

	@Override
	protected PersistentGameBehaviorType<T> createEntry() {
		return new PersistentGameBehaviorType<>(codec);
	}

	@Override
	protected PersistentGameBehaviorEntry<T> createEntryWrapper(DeferredHolder<PersistentGameBehaviorType<?>, PersistentGameBehaviorType<T>> delegate) {
		return new PersistentGameBehaviorEntry<>(getOwner(), delegate);
	}

	@Override
	public PersistentGameBehaviorEntry<T> register() {
		return (PersistentGameBehaviorEntry<T>) super.register();
	}
}
