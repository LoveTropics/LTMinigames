package org.lovetropics.games.common.core.game.behavior.instances.team;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.content.MinigameTexts;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.behavior.event.GameTeamEvents;
import org.lovetropics.games.common.core.game.player.PlayerRole;
import org.lovetropics.games.common.core.game.state.GameStateMap;
import org.lovetropics.games.common.core.game.state.statistics.PlayerKey;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;
import org.lovetropics.games.common.core.game.state.team.GameTeamKey;
import org.lovetropics.games.common.core.game.state.team.TeamSetupState;
import org.lovetropics.games.common.core.game.state.team.TeamState;

import java.util.HashMap;
import java.util.Map;

public final class TeamsBehavior implements IGameBehavior {
	public static final MapCodec<TeamsBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ScoreboardTeams.CODEC.forGetter(t -> t.scoreboardTeams)
	).apply(i, TeamsBehavior::new));

	private final ScoreboardTeams scoreboardTeams;

	private @Nullable TeamSetupState teamSetup;
	private TeamState teams;

	public TeamsBehavior(ScoreboardTeams scoreboardTeams) {
		this.scoreboardTeams = scoreboardTeams;
	}

	@Override
	public void registerState(IGamePhase game, GameStateMap phaseState, GameStateMap instanceState) {
		TeamState existingTeams = instanceState.getOrNull(TeamState.KEY);
		// We might run this behavior after teams have already been set up (for example in a sub-game), and we don't want to set it up twice
		if (existingTeams == null) {
			teamSetup = instanceState.getOrThrow(TeamSetupState.KEY);
			TeamState newTeams = teamSetup.createInitialTeamState(game);
			instanceState.register(TeamState.KEY, newTeams);
			teams = newTeams;
		} else {
			teams = existingTeams;
		}
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		teams.register(events);

		TeamSetupState teamSetup = this.teamSetup;
		if (teamSetup != null) {
			events.listen(GamePlayerEvents.BEFORE_ADD_PLAYERS, (participants, spectators) ->
					teamSetup.allocatePlayers(teams, participants)
			);
			events.listen(GamePlayerEvents.ALLOCATE_ROLES, allocator -> {
				// All players that are assigned to a team should also be forced to be participating
				teamSetup.assignedPlayers().forEach(player ->
						allocator.addPlayer(player, PlayerRole.PARTICIPANT)
				);
			});
			this.teamSetup = null;
		}

		events.listen(GamePlayerEvents.BEFORE_ADD_PLAYERS, (participants, spectators) -> {
			Map<PlayerKey, GameTeamKey> participantTeams = new HashMap<>();
			for (PlayerKey participant : participants) {
				GameTeamKey team = teams.getTeamForPlayer(participant);
				if (team != null) {
					participantTeams.put(participant, team);
				}
			}
			game.invoker(GameTeamEvents.TEAMS_ALLOCATED).onTeamsAllocated(participantTeams);
		});

		events.listen(GameTeamEvents.SET_GAME_TEAM, (player, _, team) -> {
			game.statistics().forPlayer(player).set(StatisticKey.TEAM, team.key());

			Component teamName = team.styledName().withStyle(ChatFormatting.BOLD);
			player.sendSystemMessage(MinigameTexts.ON_TEAM.apply(teamName), false);
		});

		game.statistics().global().set(StatisticKey.TEAMS, true);

		scoreboardTeams.register(game, events);
	}
}
