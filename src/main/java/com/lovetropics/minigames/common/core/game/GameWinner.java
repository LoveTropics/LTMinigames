package com.lovetropics.minigames.common.core.game;

import com.lovetropics.minigames.common.content.MinigameTexts;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionSubjects;
import com.lovetropics.minigames.common.core.game.state.statistics.PlayerKey;
import com.lovetropics.minigames.common.core.game.state.team.GameTeam;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

public sealed interface GameWinner {
	static GameWinner byPlayerKey(IGamePhase game, PlayerKey key) {
		ServerPlayer player = game.allPlayers().getPlayerBy(key);
		return player != null ? new Player(player) : new OfflinePlayer(key, Component.literal(key.name()));
	}

	Component name();

	ActionSubjects<?> resolveSubjects(IGamePhase game);

	@Nullable
	default GameTeamKey asTeam(IGamePhase game) {
		return switch (this) {
			case GameWinner.Player(ServerPlayer player) -> getTeamFor(game, PlayerKey.from(player));
			case GameWinner.Team(GameTeam team) -> team.key();
			case GameWinner.OfflinePlayer(PlayerKey playerKey, Component ignored) -> getTeamFor(game, playerKey);
			case GameWinner.Nobody ignored -> null;
		};
	}

	@Nullable
	private static GameTeamKey getTeamFor(IGamePhase game, PlayerKey player) {
		TeamState teams = game.instanceState().getOrNull(TeamState.KEY);
		return teams != null ? teams.getTeamForPlayer(player) : null;
	}

	record Nobody() implements GameWinner {
		@Override
		public Component name() {
			return MinigameTexts.NOBODY;
		}

		@Override
		public ActionSubjects<?> resolveSubjects(IGamePhase game) {
			return ActionSubjects.EMPTY;
		}
	}

	record Player(ServerPlayer player) implements GameWinner {
		@Override
		public Component name() {
			return player.getDisplayName();
		}

		@Override
		public ActionSubjects<?> resolveSubjects(IGamePhase game) {
			return ActionSubjects.ofPlayer(player);
		}
	}

	record OfflinePlayer(PlayerKey playerKey, Component name) implements GameWinner {
		@Override
		public ActionSubjects<?> resolveSubjects(IGamePhase game) {
			return ActionSubjects.EMPTY;
		}
	}

	record Team(GameTeam team) implements GameWinner {
		@Override
		public Component name() {
			return team.config().styledName();
		}

		@Override
		public ActionSubjects<?> resolveSubjects(IGamePhase game) {
			return ActionSubjects.ofTeam(team.key());
		}
	}
}
