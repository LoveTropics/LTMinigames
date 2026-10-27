package org.lovetropics.games.common.core.game.persistent;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.lovetropics.games.common.core.game.behavior.event.GameEventListeners;
import org.lovetropics.games.common.core.game.behavior.event.GameEventType;
import org.lovetropics.games.common.core.game.player.MutablePlayerSet;
import org.lovetropics.games.common.core.game.region.GameRegionHolder;
import org.lovetropics.games.common.core.game.region.GameRegions;

public interface PersistentGame extends GameRegionHolder {
	GameEventListeners events();

	<T> T invoker(GameEventType<T> type);

	MutablePlayerSet players();

	ServerLevel level();

	@Override
	default MinecraftServer server() {
		return level().getServer();
	}

	GameRegions gameRegions();
}
