package com.lovetropics.minigames.common.content.escape_race.behaviours;

import com.lovetropics.minigames.common.content.escape_race.EscapeRaceTexts;
import com.lovetropics.minigames.common.content.escape_race.event.EscapeRaceEvents;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.state.statistics.PlayerKey;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.state.team.GameTeam;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.lovetropics.minigames.common.core.game.util.GameSidebar;
import com.lovetropics.minigames.common.core.game.util.GameWidgets;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class VacationDaysBehaviour implements IGameBehavior {
	public static final MapCodec<VacationDaysBehaviour> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			StatisticKey.CODEC.listOf().optionalFieldOf("share_statistics", List.of()).forGetter(b -> b.shareStatistics)
	).apply(i, VacationDaysBehaviour::new));

	private static final int SIDEBAR_INTERVAL = SharedConstants.TICKS_PER_SECOND / 2;

	private final List<StatisticKey<?>> shareStatistics;

	public VacationDaysBehaviour(List<StatisticKey<?>> shareStatistics) {
		this.shareStatistics = shareStatistics;
	}

	private IGamePhase game;
	private TeamState teams;

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		this.game = game;
		teams = game.instanceState().getOrThrow(TeamState.KEY);

		GameSidebar sidebar = GameWidgets.getOrRegister(game, events).openGlobalSidebar(EscapeRaceTexts.SIDEBAR_VACATION_DAYS);

		Object2IntMap<GameTeamKey> lastTeamPoints = new Object2IntArrayMap<>();
		events.listen(EscapeRaceEvents.VACATION_DAYS_CHANGED, (team, value, lastValue) -> {
			PlayerSet playersForTeam = teams.getPlayersForTeam(game, team);
			int increase = value - lastValue;
			playersForTeam.sendMessage(EscapeRaceTexts.VACATION_DAYS_CHANGED.apply(increase), true);

			for (int i = 0; i < increase; i++) {
				float pitch = Mth.lerp((float) i / increase, 1.0f, 2.0f);
				game.scheduler().runAfterTicks(5 + i * 3, () ->
						playersForTeam.playSound(SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.NEUTRAL, 1.0f, pitch)
				);
			}
		});

		events.listen(GamePhaseEvents.TICK, () -> {
			for (GameTeamKey teamKey : teams.getTeamKeys()) {
				int newPoints = game.statistics().forTeam(teamKey).getInt(StatisticKey.VICTORY_POINTS);
				int oldPoints = lastTeamPoints.put(teamKey, newPoints);
				if (newPoints != oldPoints) {
					game.invoker(EscapeRaceEvents.VACATION_DAYS_CHANGED).onVacationDaysChanged(teamKey, newPoints, oldPoints);
				}
			}

			if (game.ticks() % SIDEBAR_INTERVAL == 0) {
				sidebar.set(renderSidebar(teams));
			}
		});
	}

	@Nullable
	private GameTeamKey getTeamFor(PlayerKey player) {
		TeamState teams = game.instanceState().getOrNull(TeamState.KEY);
		return teams != null ? teams.getTeamForPlayer(player) : null;
	}

	private void addPoints(final PlayerKey playerKey, final int points) {
		TeamState teams = game.instanceState().getOrNull(TeamState.KEY);
		GameTeamKey team = teams != null ? teams.getTeamForPlayer(playerKey) : null;
		if (team != null) {
			addPoints(team, points);
		}
	}

	private void addPoints(final GameTeamKey team, final int points) {
		game.statistics().forTeam(team).incrementInt(StatisticKey.VACATION_DAYS, points);
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

		sidebar.add(EscapeRaceTexts.SIDEBAR_HEADER.apply(
				Component.literal(String.valueOf(game.statistics().forTeam(firstTeam.key()).getInt(StatisticKey.VACATION_DAYS))),
				firstTeam.config().styledName(),
				Component.literal(String.valueOf(game.statistics().forTeam(secondTeam.key()).getInt(StatisticKey.VACATION_DAYS))),
				secondTeam.config().styledName()
		));

		return sidebar.toArray(new Component[0]);
	}
}
