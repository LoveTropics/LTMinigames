package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior;

import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event.BbEvents;
import com.lovetropics.minigames.common.content.biodiversity_blitz.client_state.CurrencyTargetState;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.CurrencyManager;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.PlotsState;
import com.lovetropics.minigames.common.core.game.GameWinner;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameLogicEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.state.team.GameTeam;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class BbCurrencyWinTrigger implements IGameBehavior {
	public static final MapCodec<BbCurrencyWinTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.fieldOf("threshold_currency").forGetter(c -> c.thresholdCurrency)
	).apply(i, BbCurrencyWinTrigger::new));

	private final int thresholdCurrency;

	private final List<GameTeamKey> winnerCandidates = new ArrayList<>();

	private boolean gameOver;

	public BbCurrencyWinTrigger(int thresholdCurrency) {
		this.thresholdCurrency = thresholdCurrency;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		PlotsState plots = game.state().getOrThrow(PlotsState.KEY);
		CurrencyManager currency = game.state().getOrThrow(CurrencyManager.KEY);
		TeamState teams = game.instanceState().getOrThrow(TeamState.KEY);

		CurrencyTargetState target = new CurrencyTargetState(thresholdCurrency);
		GameClientState.applyGlobally(target, events);

		events.listen(BbEvents.CURRENCY_ACCUMULATE, (team, value, lastValue) -> {
			if (value >= thresholdCurrency) {
				winnerCandidates.add(team);
			}
		});

		events.listen(GamePhaseEvents.TICK, () -> {
			if (!gameOver && !winnerCandidates.isEmpty()) {
				GameTeamKey teamKey = selectWinningTeam(game, teams, plots, currency, winnerCandidates);
				triggerWin(game, teams.getTeamByKey(teamKey));
			}
		});
	}

	private GameTeamKey selectWinningTeam(IGamePhase game, TeamState teams, PlotsState plots, CurrencyManager currency, List<GameTeamKey> candidates) {
		if (candidates.size() == 1) {
			return candidates.getFirst();
		}

		Comparator<GameTeamKey> comparator = Comparator.<GameTeamKey>comparingInt(team -> getTeamCurrencyItems(game, teams, currency, team))
				.thenComparingInt(team -> {
					Plot plot = plots.getPlotFor(team);
					return plot != null ? plot.nextCurrencyIncrement : 0;
				});

		return candidates.stream().max(comparator).orElseThrow();
	}

	private static int getTeamCurrencyItems(IGamePhase game, TeamState teams, CurrencyManager currency, GameTeamKey team) {
		return teams.getPlayersForTeam(game, team).stream()
				.mapToInt(currency::get)
				.sum();
	}

	private void triggerWin(IGamePhase game, @Nullable GameTeam team) {
		if (team != null) {
			game.invoker(GameLogicEvents.GAME_OVER).onGameWonBy(team);
		} else {
			game.invoker(GameLogicEvents.GAME_OVER).onGameOver(new GameWinner.Nobody());
		}

		gameOver = true;
	}
}
