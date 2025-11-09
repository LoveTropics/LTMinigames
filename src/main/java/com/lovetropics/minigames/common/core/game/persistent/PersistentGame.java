package com.lovetropics.minigames.common.core.game.persistent;

import com.lovetropics.minigames.common.core.game.behavior.event.GameEventListeners;
import com.lovetropics.minigames.common.core.game.behavior.event.GameEventType;
import com.lovetropics.minigames.common.core.game.player.MutablePlayerSet;
import net.minecraft.server.level.ServerLevel;

public interface PersistentGame {
	GameEventListeners events();

	<T> T invoker(GameEventType<T> type);

	MutablePlayerSet players();

	ServerLevel level();
}
