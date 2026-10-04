package org.lovetropics.games.common.core.game.persistent;

import net.minecraft.server.level.ServerLevel;
import org.lovetropics.games.common.core.game.behavior.event.GameEventListeners;
import org.lovetropics.games.common.core.game.behavior.event.GameEventType;
import org.lovetropics.games.common.core.game.player.MutablePlayerSet;

public interface PersistentGame {
	GameEventListeners events();

	<T> T invoker(GameEventType<T> type);

	MutablePlayerSet players();

	ServerLevel level();
}
