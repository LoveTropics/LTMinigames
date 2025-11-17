package com.lovetropics.minigames.common.core.game.behavior.event;

import com.lovetropics.minigames.common.core.game.IGamePhase;

public final class SubGameEvents {
	public static final GameEventType<Create> CREATE = GameEventType.create(Create.class, listeners -> (subGame, subEvents) -> {
		for (Create listener : listeners) {
			listener.onCreateSubGame(subGame, subEvents);
		}
	});

	private SubGameEvents() {
	}

	public interface Create {
		void onCreateSubGame(IGamePhase subGame, EventRegistrar subEvents);
	}
}
