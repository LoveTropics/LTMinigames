package org.lovetropics.games.common.core.game.region;

import net.minecraft.server.MinecraftServer;

public interface GameRegionHolder {
	GameRegions gameRegions();

	default int regionToken() {
		return 0;
	}

	MinecraftServer server();
}
