package com.lovetropics.minigames.common.content.river_race.microgames;

import com.lovetropics.minigames.common.content.river_race.RiverRaceTexts;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.GameWinner;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameLogicEvents;
import com.lovetropics.minigames.common.core.game.state.statistics.GameStatistics;
import com.lovetropics.minigames.common.core.game.state.statistics.Placement;
import com.lovetropics.minigames.common.core.game.state.statistics.PlacementOrder;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class MicrogameScoringBehavior implements IGameBehavior {
	public static final MapCodec<MicrogameScoringBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			StatisticKey.INT_CODEC.fieldOf("statistic").forGetter(c -> c.statistic),
			ExtraCodecs.nonEmptyList(ExtraCodecs.compactListCodec(Codec.INT)).optionalFieldOf("points_per_game_won", List.of(3, 2, 1)).forGetter(c -> c.pointsPerGameWon),
			Codec.unboundedMap(Identifier.CODEC, Codec.INT).optionalFieldOf("special_points_per_game", Map.of()).forGetter(c -> c.specialPointsPerGame)
	).apply(i, MicrogameScoringBehavior::new));

	private final StatisticKey<Integer> statistic;
	private final List<Integer> pointsPerGameWon;
	private final Map<Identifier, Integer> specialPointsPerGame;

	@Nullable
	private MicrogameSegmentState microgameSegment;

	public MicrogameScoringBehavior(StatisticKey<Integer> statistic, List<Integer> pointsPerGameWon, Map<Identifier, Integer> specialPointsPerGame) {
		this.statistic = statistic;
		this.pointsPerGameWon = pointsPerGameWon;
		this.specialPointsPerGame = specialPointsPerGame;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(MicrogameEvents.CREATE_MICROGAME, (subGame, subEvents) -> {
			if (microgameSegment == null) {
				microgameSegment = new MicrogameSegmentState();
			}
			MicrogameSegmentState segment = microgameSegment;
			subEvents.listen(GameLogicEvents.GAME_OVER, winner -> {
				Identifier microgameId = subGame.definition().id();
				onMicrogameWinTriggered(game, microgameId, winner, segment);
			});
		});
		events.listen(MicrogameEvents.MICROGAMES_ENDED, () -> {
			if (microgameSegment != null) {
				onMicrogamesCompleted(game, microgameSegment);
				microgameSegment = null;
			}
		});
	}

	private void onMicrogameWinTriggered(IGamePhase game, Identifier microgameId, GameWinner winner, MicrogameSegmentState segmentState) {
		GameTeamKey winningTeam = winner.asTeam(game);
		if (winningTeam != null) {
			int winIndex = segmentState.winCountByTeam.addTo(winningTeam, 1);
			int points = pointsPerGameWon.get(Math.min(winIndex, pointsPerGameWon.size() - 1));
			points = specialPointsPerGame.getOrDefault(microgameId, points);
			segmentState.pointsByTeam.addTo(winningTeam, points);
		}
	}

	private void onMicrogamesCompleted(IGamePhase game, MicrogameSegmentState segmentState) {
		GameStatistics segmentStatistics = new GameStatistics();

		for (Object2IntMap.Entry<GameTeamKey> entry : segmentState.pointsByTeam.object2IntEntrySet()) {
			final int points = entry.getIntValue();
			game.statistics().forTeam(entry.getKey()).incrementInt(statistic, points);
			segmentStatistics.forTeam(entry.getKey()).set(statistic, points);
		}

		game.allPlayers().sendMessage(RiverRaceTexts.MICROGAME_RESULTS);
		Placement.fromScore(game, segmentStatistics, segmentStatistics.getTeams(), statistic, PlacementOrder.MAX.asComparator(), true)
				.sendTo(game.allPlayers(), 5);
	}

	private static final class MicrogameSegmentState {
		private final Object2IntOpenHashMap<GameTeamKey> winCountByTeam = new Object2IntOpenHashMap<>();
		private final Object2IntOpenHashMap<GameTeamKey> pointsByTeam = new Object2IntOpenHashMap<>();
	}
}
