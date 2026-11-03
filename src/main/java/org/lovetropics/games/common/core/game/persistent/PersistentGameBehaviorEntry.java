package org.lovetropics.games.common.core.game.persistent;

import com.mojang.serialization.MapCodec;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class PersistentGameBehaviorEntry<T extends PersistentGameBehavior> extends RegistryEntry<MapCodec<? extends PersistentGameBehavior>, MapCodec<T>> {
	public PersistentGameBehaviorEntry(AbstractRegistrate<?> owner, DeferredHolder<MapCodec<? extends PersistentGameBehavior>, MapCodec<T>> delegate) {
		super(owner, delegate);
	}
}
