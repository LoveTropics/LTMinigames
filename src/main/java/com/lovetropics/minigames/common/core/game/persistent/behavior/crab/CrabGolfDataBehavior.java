package com.lovetropics.minigames.common.core.game.persistent.behavior.crab;

import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGame;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehavior;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehaviorType;
import com.mojang.datafixers.util.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class CrabGolfDataBehavior implements PersistentGameBehavior {
	// Hole -> (Player, Lowest score)
	private final Map<Integer, Pair<UUID, Integer>> holes = new HashMap<>();
	// TODO: player score

	@Override
	public void register(PersistentGame game, EventRegistrar events) {

	}

	public int getLowestScoreForHole(int hole) {
		Pair<UUID, Integer> res = holes.get(hole);
		if (res == null) {
			return Integer.MAX_VALUE;
		}
		return res.getSecond();
	}

	@Override
	public Supplier<? extends PersistentGameBehaviorType<?>> type() {
		return null;
	}

	// TODO: persistent data?
}
