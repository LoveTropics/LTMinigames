package org.lovetropics.games.common.content.build_competition;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.util.registry.GameBehaviorEntry;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;

public final class BuildCompetition {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final GameBehaviorEntry<PollFinalistsBehavior> POLL_FINALISTS = REGISTRATE.object("poll_finalists")
			.behavior(PollFinalistsBehavior.CODEC)
			.register();

	public static void init() {
	}
}
