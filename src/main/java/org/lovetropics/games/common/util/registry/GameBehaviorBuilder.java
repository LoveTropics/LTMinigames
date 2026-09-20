package org.lovetropics.games.common.util.registry;

import org.lovetropics.games.common.core.game.behavior.GameBehaviorType;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorTypes;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import com.mojang.serialization.MapCodec;
import com.tterrag.registrate.builders.AbstractBuilder;
import com.tterrag.registrate.builders.BuilderCallback;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class GameBehaviorBuilder<T extends IGameBehavior, P> extends AbstractBuilder<GameBehaviorType<?>, GameBehaviorType<T>, P, GameBehaviorBuilder<T, P>> {
	private final MapCodec<T> codec;

	public GameBehaviorBuilder(LoveTropicsRegistrate owner, P parent, String name, BuilderCallback callback, MapCodec<T> codec) {
		super(owner, parent, name, callback, GameBehaviorTypes.REGISTRY_KEY);
		this.codec = codec;
	}

	@Override
	protected GameBehaviorType<T> createEntry() {
		return new GameBehaviorType<>(codec);
	}

	@Override
	protected GameBehaviorEntry<T> createEntryWrapper(DeferredHolder<GameBehaviorType<?>, GameBehaviorType<T>> delegate) {
		return new GameBehaviorEntry<>(getOwner(), delegate);
	}

	@Override
	public GameBehaviorEntry<T> register() {
		return (GameBehaviorEntry<T>) super.register();
	}
}
