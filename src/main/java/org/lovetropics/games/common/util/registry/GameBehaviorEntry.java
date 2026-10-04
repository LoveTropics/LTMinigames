package org.lovetropics.games.common.util.registry;

import com.mojang.serialization.MapCodec;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorType;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;

public final class GameBehaviorEntry<T extends IGameBehavior> extends RegistryEntry<GameBehaviorType<?>, GameBehaviorType<T>> {
	public GameBehaviorEntry(AbstractRegistrate<?> owner, DeferredHolder<GameBehaviorType<?>, GameBehaviorType<T>> delegate) {
		super(owner, delegate);
	}

	public MapCodec<T> getCodec() {
		return get().codec();
	}
}
