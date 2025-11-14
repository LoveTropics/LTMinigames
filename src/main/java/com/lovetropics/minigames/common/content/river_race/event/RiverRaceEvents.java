package com.lovetropics.minigames.common.content.river_race.event;

import com.lovetropics.minigames.common.content.river_race.block.TriviaType;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.event.GameEventType;
import com.lovetropics.minigames.common.core.game.state.team.GameTeam;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public class RiverRaceEvents {

	public static final GameEventType<AnswerTriviaQuestion> QUESTION_COMPLETED = GameEventType.create(AnswerTriviaQuestion.class, listeners -> (player, triviaType, triviaPos) -> {
		for (AnswerTriviaQuestion listener : listeners) {
			listener.onAnswer(player, triviaType, triviaPos);
		}
	});

	public static final GameEventType<VictoryPointsChanged> VICTORY_POINTS_CHANGED = GameEventType.create(VictoryPointsChanged.class, listeners -> (team, value, lastValue) -> {
		for (VictoryPointsChanged listener : listeners) {
			listener.onVictoryPointsChanged(team, value, lastValue);
		}
	});

	public static final GameEventType<CollectablePlaced> COLLECTABLE_PLACED = GameEventType.create(CollectablePlaced.class, listeners -> (player, team, pos) -> {
		for (CollectablePlaced listener : listeners) {
			listener.onCollectablePlaced(player, team, pos);
		}
	});

	public static final GameEventType<UnlockZone> UNLOCK_ZONE = GameEventType.create(UnlockZone.class, listeners -> id -> {
		for (UnlockZone listener : listeners) {
			listener.onUnlockZone(id);
		}
	});

	public static final GameEventType<ModifyMaxSpawnCount> MODIFY_MAX_SPAWN_COUNT = GameEventType.create(ModifyMaxSpawnCount.class, listeners -> (pos, count) -> {
		for (ModifyMaxSpawnCount listener : listeners) {
			count = listener.modifyMaxSpawnCount(pos, count);
		}
		return count;
	});

	public interface AnswerTriviaQuestion {
		void onAnswer(ServerPlayer player, TriviaType triviaType, BlockPos triviaPos);
	}

	public interface VictoryPointsChanged {
		void onVictoryPointsChanged(GameTeamKey team, int value, int lastValue);
	}

	public interface CollectablePlaced {
		void onCollectablePlaced(ServerPlayer player, GameTeam team, BlockPos pos);
	}

	public interface UnlockZone {
		void onUnlockZone(String id);
	}

	public interface ModifyMaxSpawnCount {
		int modifyMaxSpawnCount(BlockPos pos, int count);
	}
}
