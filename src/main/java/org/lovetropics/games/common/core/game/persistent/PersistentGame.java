package org.lovetropics.games.common.core.game.persistent;

import org.lovetropics.games.common.core.game.behavior.event.GameEventListeners;
import org.lovetropics.games.common.core.game.behavior.event.GameEventType;
import org.lovetropics.games.common.core.game.player.MutablePlayerSet;
import net.minecraft.server.level.ServerLevel;

public interface PersistentGame {
	GameEventListeners events();

	<T> T invoker(GameEventType<T> type);

	MutablePlayerSet players();

	ServerLevel level();
}
