package com.lovetropics.minigames.common.core.game;

import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.player.PlayerIterable;
import net.minecraft.server.level.ServerPlayer;

public interface PendingSubPhase {
	void queuePlayer(ServerPlayer player);

	default void queuePlayers(PlayerIterable players) {
		players.forEach(this::queuePlayer);
	}

	void whenCreated(CreateHandler handler);

	interface CreateHandler {
		void onCreate(IGamePhase subGame, EventRegistrar subEvents);
	}
}
