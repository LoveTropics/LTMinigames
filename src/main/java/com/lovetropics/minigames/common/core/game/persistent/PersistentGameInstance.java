package com.lovetropics.minigames.common.core.game.persistent;

import com.lovetropics.minigames.common.core.game.behavior.event.GameEventListeners;
import com.lovetropics.minigames.common.core.game.behavior.event.GameEventType;
import com.lovetropics.minigames.common.core.game.player.MutablePlayerSet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public class PersistentGameInstance implements PersistentGame {
	private final MutablePlayerSet players;
	private final GameEventListeners events = new GameEventListeners();
	private final MinecraftServer server;
	private final ServerLevel level;

	public PersistentGameInstance(MinecraftServer server, ServerLevel level) {
		this.players = new MutablePlayerSet(server);
		this.server = server;
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
