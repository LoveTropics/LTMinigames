package org.lovetropics.games.common.core.game.persistent;

import org.lovetropics.games.common.core.game.behavior.event.GameEventListeners;
import org.lovetropics.games.common.core.game.behavior.event.GameEventType;
import org.lovetropics.games.common.core.game.player.MutablePlayerSet;
import net.minecraft.server.level.ServerLevel;

public class PersistentGameInstance implements PersistentGame {
	private final MutablePlayerSet players = new MutablePlayerSet();
	private final GameEventListeners events = new GameEventListeners();
	private final ServerLevel level;

	public PersistentGameInstance(ServerLevel level) {
		this.level = level;
	}

	@Override
	public GameEventListeners events() {
		return events;
	}

	@Override
	public <T> T invoker(GameEventType<T> type) {
		return events.invoker(type);
	}

	@Override
	public MutablePlayerSet players() {
		return players;
	}

	@Override
	public ServerLevel level() {
		return level;
	}
}
