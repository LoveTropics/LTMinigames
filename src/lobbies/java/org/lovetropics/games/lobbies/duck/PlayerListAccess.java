package org.lovetropics.games.lobbies.duck;

import net.minecraft.server.level.ServerPlayer;

public interface PlayerListAccess {
	void ltminigames$save(ServerPlayer player);

	void ltminigames$remove(ServerPlayer player);

	void ltminigames$add(ServerPlayer player);

	void ltminigames$firePlayerLoading(ServerPlayer player);
}
