package org.lovetropics.games.common.core.game.behavior.action;

import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.state.team.GameTeam;
import org.lovetropics.games.common.core.game.state.team.GameTeamKey;
import org.lovetropics.games.common.core.game.state.team.TeamState;

import java.util.List;
import java.util.Set;

public class ActionSubjectDescriber {
	public static @Nullable Component describe(IGamePhase game, ActionSubjects<?> targets, @Nullable ChatFormatting playerNameStyle) {
		List<ServerPlayer> targetPlayers = targets.asPlayers(game);
		if (targetPlayers.isEmpty() || Set.copyOf(targetPlayers).containsAll(Sets.newHashSet(game.participants()))) {
			return null;
		}

		// Try to figure out the intended target of the package, if we can find teams that fully match this target list, that is likely most relevant
		// Intentionally not using a strong dependency on the package configuration to keep this flexible
		Set<GameTeam> targetTeams = teamsExactlyMatching(game, targetPlayers);
		if (targetTeams != null) {
			return ComponentUtils.formatList(targetTeams, GameTeam::styledName);
		}

		return ComponentUtils.formatList(targetPlayers, p -> playerNameStyle != null ? p.getDisplayName().copy().withStyle(playerNameStyle) : p.getDisplayName());
	}

	private static @Nullable Set<GameTeam> teamsExactlyMatching(IGamePhase game, List<ServerPlayer> players) {
		TeamState teams = game.instanceState().getOrNull(TeamState.KEY);
		if (teams == null) {
			return null;
		}
		Set<GameTeam> exactTeams = new ReferenceArraySet<>();
		for (ServerPlayer player : players) {
			GameTeamKey key = teams.getTeamForPlayer(player);
			GameTeam team = key != null ? teams.getTeamByKey(key) : null;
			if (team == null) {
				return null;
			}
			if (teams.getParticipantsForTeam(game, key).stream().anyMatch(otherPlayer -> !players.contains(otherPlayer))) {
				// There are other players in this team, but not all of them are included
				return null;
			}
			exactTeams.add(team);
		}
		return exactTeams;
	}
}
