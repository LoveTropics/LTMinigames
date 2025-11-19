package com.lovetropics.minigames.common.core.game;

import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.player.PlayerIterable;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

public interface PendingSubPhase {
	void queuePlayer(ServerPlayer player);

	default void queuePlayers(PlayerIterable players) {
		players.forEach(this::queuePlayer);
	}

	void whenCreated(CreateHandler handler);

	// TODO: Can we refactor to not require users to know about errors?
	void whenErrored(Consumer<Exception> consumer);

	interface CreateHandler {
		void onCreate(IGamePhase subGame, EventRegistrar subEvents);
	}
}
