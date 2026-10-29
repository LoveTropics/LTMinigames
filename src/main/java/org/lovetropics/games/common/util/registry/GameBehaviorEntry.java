package org.lovetropics.games.common.util.registry;

import com.mojang.serialization.MapCodec;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;

public final class GameBehaviorEntry<T extends IGameBehavior> extends RegistryEntry<MapCodec<? extends IGameBehavior>, MapCodec<T>> {
	public GameBehaviorEntry(AbstractRegistrate<?> owner, DeferredHolder<MapCodec<? extends IGameBehavior>, MapCodec<T>> delegate) {
		super(owner, delegate);
	}
}
