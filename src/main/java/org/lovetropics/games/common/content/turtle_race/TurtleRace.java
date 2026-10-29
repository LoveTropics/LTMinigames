package org.lovetropics.games.common.content.turtle_race;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;

public final class TurtleRace {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static void init() {
		REGISTRATE.registerBehavior("rider", RiderBehavior.CODEC);
		REGISTRATE.registerBehavior("turtle_boost", TurtleBoostBehavior.CODEC);
		REGISTRATE.registerBehavior("race_track", RaceTrackBehavior.CODEC);
	}
}
