package com.lovetropics.minigames.common.core.game.persistent.behavior.crab;

import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGame;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehavior;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehaviorType;

import java.util.function.Supplier;

public class CrabGolfDummyPlayerBehavior implements PersistentGameBehavior {
	private int hole;
	private String dummyRegion;

	@Override
	public void register(PersistentGame game, EventRegistrar events) {

	}

	@Override
	public Supplier<? extends PersistentGameBehaviorType<?>> type() {
		return null;
	}
}
