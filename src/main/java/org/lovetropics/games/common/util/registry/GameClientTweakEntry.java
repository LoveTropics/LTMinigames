package org.lovetropics.games.common.util.registry;

import com.mojang.serialization.MapCodec;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateType;

public final class GameClientTweakEntry<T extends GameClientState> extends RegistryEntry<GameClientStateType<?>, GameClientStateType<T>> {
	public GameClientTweakEntry(AbstractRegistrate<?> owner, DeferredHolder<GameClientStateType<?>, GameClientStateType<T>> delegate) {
		super(owner, delegate);
	}

	public MapCodec<T> getCodec() {
		return get().codec();
	}
}
