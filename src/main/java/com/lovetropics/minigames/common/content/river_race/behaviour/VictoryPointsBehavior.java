package com.lovetropics.minigames.common.content.river_race.behaviour;

import com.lovetropics.minigames.common.content.river_race.RiverRaceState;
import com.lovetropics.minigames.common.content.river_race.RiverRaceTexts;
import com.lovetropics.minigames.common.content.river_race.block.TriviaType;
import com.lovetropics.minigames.common.content.river_race.event.RiverRaceEvents;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.GameWinner;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameLogicEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.SubGameEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.state.statistics.GameStatistics;
import com.lovetropics.minigames.common.core.game.state.statistics.Placement;
import com.lovetropics.minigames.common.core.game.state.statistics.PlacementOrder;
import com.lovetropics.minigames.common.core.game.state.statistics.PlayerKey;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.state.team.GameTeam;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.lovetropics.minigames.common.core.game.util.GameSidebar;
import com.lovetropics.minigames.common.core.game.util.GlobalGameWidgets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class VictoryPointsBehavior implements IGameBehavior {

	public static final MapCodec<VictoryPointsBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.optionalFieldOf("trivia_chest_points", 1).forGetter(c -> c.triviaChestPoints),
			Codec.INT.optionalFieldOf("trivia_gate_points", 2).forGetter(c -> c.triviaGatePoints),
			Codec.INT.optionalFieldOf("trivia_challenge_points", 5).forGetter(c -> c.triviaChallengePoints),
			Codec.INT.optionalFieldOf("collectable_collected_points", 0).forGetter(c -> c.collectableCollectedPoints),
			Codec.INT.optionalFieldOf("collectable_placed_points", 1).forGetter(c -> c.collectablePlacedPoints),
			ExtraCodecs.nonEmptyList(ExtraCodecs.compactListCodec(Codec.INT)).optionalFieldOf("points_per_game_won", List.of(3, 2, 1)).forGetter(c -> c.pointsPerGameWon),
			Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("special_points_per_game", Map.of()).forGetter(c -> c.specialPointsPerGame)
	).apply(i, VictoryPointsBehavior::new));

	private static final int SIDEBAR_INTERVAL = SharedConstants.TICKS_PER_SECOND / 2;

	private IGamePhase game;
	private TeamState teams;
	private RiverRaceState riverRace;
	private final Object2IntMap<String> availablePointsPerZone = new Object2IntOpenHashMap<>();
	private final Map<GameTeamKey, Object2IntOpenHashMap<String>> acquiredPointsPerZone = new HashMap<>();

	private final int triviaChestPoints;
	private final int triviaGatePoints;
	private final int triviaChallengePoints;
	private final int collectableCollectedPoints;
	private final int collectablePlacedPoints;
	private final List<Integer> pointsPerGameWon;
	private final Map<ResourceLocation, Integer> specialPointsPerGame;

	@Nullable
	private MicrogameSegmentState microgameSegment;

	public VictoryPointsBehavior(int triviaChestPoints, int triviaGatePoints, int triviaChallengePoints, int collectableCollectedPoints, int collectablePlacedPoints, List<Integer> pointsPerGameWon, Map<ResourceLocation, Integer> specialPointsPerGame) {
		this.triviaChestPoints = triviaChestPoints;
		this.triviaGatePoints = triviaGatePoints;
		this.triviaChallengePoints = triviaChallengePoints;
		this.collectableCollectedPoints = collectableCollectedPoints;
		this.collectablePlacedPoints = collectablePlacedPoints;
		this.pointsPerGameWon = pointsPerGameWon;
		this.specialPointsPerGame = specialPointsPerGame;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		this.game = game;
		teams = game.instanceState().getOrThrow(TeamState.KEY);

		riverRace = game.state().get(RiverRaceState.KEY);
		for (RiverRaceState.Zone zone : riverRace.getZones()) {
			availablePointsPerZone.put(zone.id(), computeAvailablePoints(zone));
		}

		for (GameTeam team : teams) {
			acquiredPointsPerZone.put(team.key(), new Object2IntOpenHashMap<>());
		}

		GameSidebar sidebar = GlobalGameWidgets.registerTo(game, events).openSidebar(RiverRaceTexts.SIDEBAR_VICTORY_POINTS);

		events.listen(RiverRaceEvents.QUESTION_COMPLETED, this::onQuestionAnswered);
		events.listen(RiverRaceEvents.COLLECTABLE_PLACED, this::onCollectablePlaced);
		events.listen(RiverRaceEvents.VICTORY_POINTS_CHANGED, (team, value, lastValue) -> {
			PlayerSet playersForTeam = teams.getPlayersForTeam(game, team);
			int increase = value - lastValue;
			playersForTeam.sendMessage(RiverRaceTexts.VICTORY_POINT_CHANGE.apply(increase), true);

			for (int i = 0; i < increase; i++) {
				float pitch = Mth.lerp((float) i / increase, 1.0f, 2.0f);
				game.scheduler().runAfterTicks(5 + i * 3, () ->
						playersForTeam.playSound(SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.NEUTRAL, 1.0f, pitch)
				);
			}
		});

		Object2IntMap<GameTeamKey> lastTeamPoints = new Object2IntArrayMap<>();
		events.listen(GamePhaseEvents.TICK, () -> {
			for (GameTeamKey teamKey : teams.getTeamKeys()) {
				int newPoints = game.statistics().forTeam(teamKey).getInt(StatisticKey.VICTORY_POINTS);
				int oldPoints = lastTeamPoints.put(teamKey, newPoints);
				if (newPoints != oldPoints) {
					game.invoker(RiverRaceEvents.VICTORY_POINTS_CHANGED).onVictoryPointsChanged(teamKey, newPoints, oldPoints);
				}
			}

			if (game.ticks() % SIDEBAR_INTERVAL == 0) {
				sidebar.set(renderSidebar(teams));
			}
		});

		events.listen(SubGameEvents.CREATE, (subGame, subEvents) -> {
			if (microgameSegment == null) {
				microgameSegment = new MicrogameSegmentState();
			}
			MicrogameSegmentState segment = microgameSegment;
			ResourceLocation microgameId = subGame.definition().id();
			subEvents.listen(GameLogicEvents.GAME_OVER, winner -> onMicrogameWinTriggered(microgameId, winner, segment));
		});
		events.listen(SubGameEvents.RETURN_TO_TOP, () -> {
			if (microgameSegment != null) {
				onMicrogamesCompleted(microgameSegment);
				microgameSegment = null;
			}
		});
	}

	private int computeAvailablePoints(RiverRaceState.Zone zone) {
		int availablePoints = 0;
		for (TriviaType type : zone.triviaBlocks().values()) {
			availablePoints += getPointsForTriviaType(type);
		}
		if (zone.collectable() != null) {
			availablePoints += collectablePlacedPoints * teams.size();
		}
		if (availablePoints % teams.size() != 0) {
			throw new GameException(Component.literal("Uneven point balance between teams"));
		}
		return availablePoints / teams.size();
	}

	private void onMicrogameWinTriggered(ResourceLocation microgameId, GameWinner winner, MicrogameSegmentState segmentState) {
		GameTeamKey winningTeam = switch (winner) {
			case GameWinner.Player(ServerPlayer player) -> getTeamFor(PlayerKey.from(player));
			case GameWinner.Team(GameTeam team) -> team.key();
			case GameWinner.OfflinePlayer(PlayerKey playerKey, Component ignored) -> getTeamFor(playerKey);
			case GameWinner.Nobody ignored -> null;
		};
		if (winningTeam != null) {
			int winIndex = segmentState.winCountByTeam.addTo(winningTeam, 1);
			int points = pointsPerGameWon.get(Math.min(winIndex, pointsPerGameWon.size() - 1));
			points = specialPointsPerGame.getOrDefault(microgameId, points);
			segmentState.pointsByTeam.addTo(winningTeam, points);
		}
	}

	private void onMicrogamesCompleted(MicrogameSegmentState segmentState) {
		GameStatistics segmentStatistics = new GameStatistics();

		for (Object2IntMap.Entry<GameTeamKey> entry : segmentState.pointsByTeam.object2IntEntrySet()) {
			addPoints(entry.getKey(), entry.getIntValue(), false);
			segmentStatistics.forTeam(entry.getKey()).set(StatisticKey.VICTORY_POINTS, entry.getIntValue());
		}

		game.allPlayers().sendMessage(RiverRaceTexts.MICROGAME_RESULTS);
		Placement.fromScore(game, segmentStatistics, segmentStatistics.getTeams(), StatisticKey.VICTORY_POINTS, PlacementOrder.MAX.asComparator())
				.sendTo(game.allPlayers(), 5);
	}

	@Nullable
	private GameTeamKey getTeamFor(PlayerKey player) {
		TeamState teams = game.instanceState().getOrNull(TeamState.KEY);
		return teams != null ? teams.getTeamForPlayer(player) : null;
	}

	private void addPoints(final PlayerKey playerKey, final int points, boolean inZone) {
		TeamState teams = game.instanceState().getOrNull(TeamState.KEY);
		GameTeamKey team = teams != null ? teams.getTeamForPlayer(playerKey) : null;
		if (team != null) {
			addPoints(team, points, inZone);
		}
	}

	private void addPoints(final GameTeamKey team, final int points, final boolean inZone) {
		game.statistics().forTeam(team).incrementInt(StatisticKey.VICTORY_POINTS, points);
		if (inZone) {
			acquiredPointsPerZone.get(team).addTo(riverRace.currentZone().id(), points);
		}
	}

	private void onQuestionAnswered(ServerPlayer player, TriviaType triviaType, BlockPos triviaPos) {
		addPoints(PlayerKey.from(player), getPointsForTriviaType(triviaType), true);
	}

	private int getPointsForTriviaType(TriviaType triviaType) {
		return switch (triviaType) {
			case COLLECTABLE -> collectableCollectedPoints;
			case VICTORY -> triviaChallengePoints;
			case REWARD -> triviaChestPoints;
			case GATE -> triviaGatePoints;
		};
	}

	private void onCollectablePlaced(ServerPlayer player, GameTeam team, BlockPos pos) {
		addPoints(PlayerKey.from(player), collectablePlacedPoints, true);
	}

	private Component[] renderSidebar(TeamState teams) {
		List<Component> sidebar = new ArrayList<>(10);

		if (teams.size() != 2) {
			// :(
			return new Component[0];
		}

		Iterator<GameTeam> iterator = teams.iterator();
		GameTeam firstTeam = iterator.next();
		GameTeam secondTeam = iterator.next();

		sidebar.add(RiverRaceTexts.SIDEBAR_HEADER.apply(
				Component.literal(String.valueOf(game.statistics().forTeam(firstTeam.key()).getInt(StatisticKey.VICTORY_POINTS))),
				firstTeam.config().styledName(),
				Component.literal(String.valueOf(game.statistics().forTeam(secondTeam.key()).getInt(StatisticKey.VICTORY_POINTS))),
				secondTeam.config().styledName()
		));

		for (RiverRaceState.Zone zone : riverRace.getZones()) {
			int pointsInZone = availablePointsPerZone.getInt(zone.id());
			if (pointsInZone == 0) {
				continue;
			}
			sidebar.add(CommonComponents.EMPTY);
			int firstPercent = getPercentInZone(zone, firstTeam, pointsInZone);
			int secondPercent = getPercentInZone(zone, secondTeam, pointsInZone);
			sidebar.add(zone.displayName());
			sidebar.add(RiverRaceTexts.SIDEBAR_TEAM_PROGRESS.apply(
					Component.literal(String.valueOf(firstPercent)).withStyle(firstTeam.config().formatting()),
					Component.literal(String.valueOf(secondPercent)).withStyle(secondTeam.config().formatting())
			));
		}

		return sidebar.toArray(new Component[0]);
	}

	private int getPercentInZone(RiverRaceState.Zone zone, GameTeam team, int totalPoints) {
		int acquiredPoints = acquiredPointsPerZone.get(team.key()).getInt(zone.id());
		return acquiredPoints * 100 / totalPoints;
	}

	private static final class MicrogameSegmentState {
		private final Object2IntOpenHashMap<GameTeamKey> winCountByTeam = new Object2IntOpenHashMap<>();
		private final Object2IntOpenHashMap<GameTeamKey> pointsByTeam = new Object2IntOpenHashMap<>();
	}
}
