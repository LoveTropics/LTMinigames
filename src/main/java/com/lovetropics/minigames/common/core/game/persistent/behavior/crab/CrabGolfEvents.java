package com.lovetropics.minigames.common.core.game.persistent.behavior.crab;

import com.lovetropics.minigames.common.core.game.behavior.event.GameEventType;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.datafixers.util.Pair;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

import java.util.UUID;

public class CrabGolfEvents {
	public static final GameEventType<StartGame> START_GAME = GameEventType.create(StartGame.class, listeners -> (hole, player) -> {
		for (StartGame listener : listeners) {
			listener.onStart(hole, player);
		}
	});

	public static final GameEventType<WinGame> WIN_GAME = GameEventType.create(WinGame.class, listeners -> (hole, player, score) -> {
		for (WinGame listener : listeners) {
			listener.onWin(hole, player, score);
		}
	});

	public interface StartGame {
		void onStart(int hole, ServerPlayer player);
	}

	public interface WinGame {
		void onWin(int hole, ServerPlayer player, int score);
	}

	public interface QueryData {
		Pair<UUID, Integer> onQuery(int hole);
	}
}
