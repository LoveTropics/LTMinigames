package org.lovetropics.games.common.core.game.region;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public interface GameRegionHolder {
	GameRegions gameRegions();

	ServerLevel level();

	MinecraftServer server();
}
