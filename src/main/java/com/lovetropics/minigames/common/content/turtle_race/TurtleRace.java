package com.lovetropics.minigames.common.content.turtle_race;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.util.registry.GameBehaviorEntry;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;

public final class TurtleRace {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final GameBehaviorEntry<RiderBehavior> RIDER = REGISTRATE.object("rider")
			.behavior(RiderBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<TurtleBoostBehavior> TURTLE_BOOST = REGISTRATE.object("turtle_boost")
			.behavior(TurtleBoostBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<RaceTrackBehavior> RACE_TRACK = REGISTRATE.object("race_track")
			.behavior(RaceTrackBehavior.CODEC)
			.register();

	public static void init() {
	}
}
