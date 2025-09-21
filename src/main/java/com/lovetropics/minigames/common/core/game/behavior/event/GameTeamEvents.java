package com.lovetropics.minigames.common.core.game.behavior.event;

import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import net.minecraft.server.level.ServerPlayer;

public final class GameTeamEvents {
	public static final GameEventType<SetGameTeam> SET_GAME_TEAM = GameEventType.create(SetGameTeam.class, listeners -> (player, teams, team) -> {
		for (SetGameTeam listener : listeners) {
			listener.onSetGameTeam(player, teams, team);
		}
	});

	public static final GameEventType<RemoveFromTeam> REMOVE_FROM_TEAM = GameEventType.create(RemoveFromTeam.class, listeners -> (player, teams, team) -> {
		for (RemoveFromTeam listener : listeners) {
			listener.onRemoveFromTeam(player, teams, team);
		}
	});

	public static final GameEventType<TeamsAllocated> TEAMS_ALLOCATED = GameEventType.create(TeamsAllocated.class, listeners -> () -> {
		for (TeamsAllocated listener : listeners) {
			listener.onTeamsAllocated();
		}
	});

	private GameTeamEvents() {
	}

	public interface SetGameTeam {
		void onSetGameTeam(ServerPlayer player, TeamState teams, GameTeamKey team);
	}

	public interface RemoveFromTeam {
		void onRemoveFromTeam(ServerPlayer player, TeamState teams, GameTeamKey team);
	}

	public interface TeamsAllocated {
		void onTeamsAllocated();
	}
}
