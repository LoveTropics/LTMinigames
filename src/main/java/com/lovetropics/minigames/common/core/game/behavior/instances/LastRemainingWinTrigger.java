package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.GameWinner;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameLogicEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.state.team.GameTeam;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;

import org.jspecify.annotations.Nullable;

public class LastRemainingWinTrigger implements IGameBehavior {
	public static final MapCodec<LastRemainingWinTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.BOOL.optionalFieldOf("last_team", false).forGetter(t -> t.lastTeam)
	).apply(i, LastRemainingWinTrigger::new));

	private final boolean lastTeam;

	private boolean eliminatedLastTick;
	private boolean winTriggered;

	public LastRemainingWinTrigger(boolean lastTeam) {
		this.lastTeam = lastTeam;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GamePlayerEvents.SET_ROLE, (player, role, lastRole) -> {
			if (lastRole == PlayerRole.PARTICIPANT && !winTriggered) {
				eliminatedLastTick = true;
			}
		});

		TeamState teams = lastTeam ? game.instanceState().getOrThrow(TeamState.KEY) : null;
		events.listen(GamePhaseEvents.TICK, () -> {
			if (!eliminatedLastTick) {
				return;
			}
			eliminatedLastTick = false;
			GameWinner winner = tryFindWinner(game, teams);
			if (winner != null) {
				game.invoker(GameLogicEvents.GAME_OVER).onGameOver(winner);
				winTriggered = true;
			}
		});
	}

	private @Nullable GameWinner tryFindWinner(IGamePhase game, @Nullable TeamState teams) {
		if (lastTeam && teams != null) {
			return tryFindTeamsWinner(game, teams);
		} else {
			return tryFindIndividualWinner(game);
		}
	}

	private static @Nullable GameWinner tryFindIndividualWinner(IGamePhase game) {
		PlayerSet participants = game.participants();
		if (participants.isEmpty()) {
			return new GameWinner.Nobody();
		} else if (participants.size() == 1) {
			ServerPlayer winningPlayer = participants.iterator().next();
			return new GameWinner.Player(winningPlayer);
		}
		return null;
	}

	private static @Nullable GameWinner tryFindTeamsWinner(IGamePhase game, TeamState teams) {
		GameTeam finalTeam = getFinalTeam(teams, game);
		if (finalTeam != null) {
			return new GameWinner.Team(finalTeam);
		} else if (game.participants().isEmpty()) {
			return new GameWinner.Nobody();
		}
		return null;
	}

	private static @Nullable GameTeam getFinalTeam(TeamState teams, IGamePhase game) {
		GameTeam finalTeam = null;
		for (GameTeam team : teams) {
			if (teams.getParticipantsForTeam(game, team.key()).isEmpty()) {
				continue;
			}
			if (finalTeam != null) {
				return null;
			} else {
				finalTeam = team;
			}
		}
		return finalTeam;
	}
}
