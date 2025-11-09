package com.lovetropics.minigames.common.core.game.persistent.behavior;

import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGame;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehavior;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehaviorType;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehaviors;
import com.mojang.serialization.MapCodec;

import java.util.function.Supplier;

public class NoOpBehavior implements PersistentGameBehavior {
	public static final MapCodec<NoOpBehavior> CODEC = MapCodec.unit(NoOpBehavior::new);

	@Override
	public void register(PersistentGame game, EventRegistrar events) {

	}

	@Override
	public Supplier<? extends PersistentGameBehaviorType<?>> type() {
		return PersistentGameBehaviors.NOOP;
	}
}
