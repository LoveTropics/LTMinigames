package com.lovetropics.minigames.common.core.game.behavior.event;

import com.lovetropics.minigames.common.core.game.IPollingGame;
import com.lovetropics.minigames.common.core.game.PlayerRole;
import net.minecraft.entity.player.ServerPlayerEntity;

import javax.annotation.Nullable;

public final class GamePollingEvents {
	public static final GameEventType<Start> START = GameEventType.create(Start.class, listeners -> game -> {
		for (Start listener : listeners) {
			listener.start(game);
		}
	});

	public static final GameEventType<PlayerRegister> PLAYER_REGISTER = GameEventType.create(PlayerRegister.class, listeners -> (game, player, role) -> {
		for (PlayerRegister listener : listeners) {
			listener.onPlayerRegister(game, player, role);
		}
	});

	private GamePollingEvents() {
	}

	public interface Start {
		void start(IPollingGame game);
	}

	public interface PlayerRegister {
		void onPlayerRegister(IPollingGame game, ServerPlayerEntity player, @Nullable PlayerRole role);
	}
}
