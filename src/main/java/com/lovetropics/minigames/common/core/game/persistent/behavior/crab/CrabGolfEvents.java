package com.lovetropics.minigames.common.core.game.persistent.behavior.crab;

import com.lovetropics.minigames.common.core.game.behavior.event.GameEventType;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.datafixers.util.Pair;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

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

	public static final GameEventType<WinGame> HIGH_SCORE = GameEventType.create(WinGame.class, listeners -> (hole, player, score) -> {
		for (WinGame listener : listeners) {
			listener.onWin(hole, player, score);
		}
	});

	public static final GameEventType<QueryPlaying> QUERY_PLAYING = GameEventType.create(QueryPlaying.class, listeners -> (player) -> {
		for (QueryPlaying listener : listeners) {
			if (listener.isPlaying(player)) {
				return true;
			}
		}

		return false;
	});

	public static final GameEventType<SummonCrab> SUMMON_CRAB = GameEventType.create(SummonCrab.class, listeners -> (level, pos) -> {
		for (SummonCrab listener : listeners) {
			LivingEntity summon = listener.summon(level, pos);
			if (summon != null) {
				return summon;
			}
		}

		return null;
	});

	public interface StartGame {
		void onStart(int hole, ServerPlayer player);
	}

	public interface WinGame {
		void onWin(int hole, ServerPlayer player, int score);
	}

	public interface SummonCrab {
		LivingEntity summon(ServerLevel level, Vec3 pos);
	}

	public interface QueryData {
		Pair<UUID, Integer> onQuery(int hole);
	}

	public interface QueryPlaying {
		boolean isPlaying(ServerPlayer player);
	}
}
