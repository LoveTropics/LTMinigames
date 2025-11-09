package com.lovetropics.minigames.common.core.game.persistent;

import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.mojang.serialization.MapCodec;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class PersistentGameBehaviorEntry<T extends PersistentGameBehavior> extends RegistryEntry<PersistentGameBehaviorType<?>, PersistentGameBehaviorType<T>> {
	public PersistentGameBehaviorEntry(AbstractRegistrate<?> owner, DeferredHolder<PersistentGameBehaviorType<?>, PersistentGameBehaviorType<T>> delegate) {
		super(owner, delegate);
	}

	public MapCodec<T> getCodec() {
		return get().codec();
	}
}
