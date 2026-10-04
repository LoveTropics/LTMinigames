package org.lovetropics.games.common.core.game.persistent;

import net.minecraft.server.level.ServerLevel;
import org.lovetropics.games.common.core.game.GameRegions;
import org.lovetropics.games.common.core.game.behavior.event.GameEventListeners;
import org.lovetropics.games.common.core.game.behavior.event.GameEventType;
import org.lovetropics.games.common.core.game.player.MutablePlayerSet;
import org.lovetropics.games.common.core.map.SavedRegions;

public class PersistentGameInstance implements PersistentGame {
	private final MutablePlayerSet players = new MutablePlayerSet();
	private final GameEventListeners events = new GameEventListeners();
	private final ServerLevel level;
	private final GameRegions regions;

	public PersistentGameInstance(ServerLevel level) {
		this.level = level;
		regions = SavedRegions.get(level).regions().compile();
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

	@Override
	public GameRegions regions() {
		return regions;
	}
}
