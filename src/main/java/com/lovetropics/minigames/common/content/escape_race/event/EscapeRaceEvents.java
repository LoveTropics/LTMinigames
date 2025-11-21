package com.lovetropics.minigames.common.content.escape_race.event;

import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.DdrLevel;
import com.lovetropics.minigames.common.core.game.behavior.event.GameEventType;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;

public class EscapeRaceEvents {

	public static final GameEventType<DDRLevelCompleted> DDR_LEVEL_COMPLETED = GameEventType.create(DDRLevelCompleted.class, listeners -> (player, level, score, bestStreak) -> {
		for (DDRLevelCompleted listener : listeners) {
			listener.onComplete(player, level, score, bestStreak);
		}
	});

	public static final GameEventType<BlockRooms> BLOCK_ROOMS = GameEventType.create(BlockRooms.class, listeners -> locked -> {
		for (BlockRooms listener : listeners) {
			listener.setRoomsBlocked(locked);
		}
	});

	public interface DDRLevelCompleted {
		void onComplete(ServerPlayer player, Holder<DdrLevel> level, int score, int bestStreak);
	}

	public interface BlockRooms {
		void setRoomsBlocked(boolean blocked);
	}
}
