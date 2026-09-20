package org.lovetropics.games.common.core.game.util;

import net.minecraft.server.level.ServerPlayer;

public interface GameWidget extends AutoCloseable {
	void addPlayer(ServerPlayer player);

	void removePlayer(ServerPlayer player);

	@Override
	void close();
}
