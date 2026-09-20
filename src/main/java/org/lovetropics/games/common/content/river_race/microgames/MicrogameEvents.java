package org.lovetropics.games.common.content.river_race.microgames;

import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameEventType;

public class MicrogameEvents {
	public static final GameEventType<CreateMicrogame> CREATE_MICROGAME = GameEventType.create(CreateMicrogame.class, listeners -> (subGame, subEvents) -> {
		for (CreateMicrogame listener : listeners) {
			listener.onCreateMicrogame(subGame, subEvents);
		}
	});

	public static final GameEventType<MicrogamesEnded> MICROGAMES_ENDED = GameEventType.create(MicrogamesEnded.class, listeners -> () -> {
		for (MicrogamesEnded listener : listeners) {
			listener.onMicrogamesEnded();
		}
	});

	public interface CreateMicrogame {
		void onCreateMicrogame(IGamePhase subGame, EventRegistrar events);
	}

	public interface MicrogamesEnded {
		void onMicrogamesEnded();
	}
}
