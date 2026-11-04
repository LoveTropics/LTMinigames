package org.lovetropics.games.common.core.game;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.lovetropics.games.common.content.MinigameTexts;
import org.lovetropics.games.common.core.game.behavior.action.ActionSubjects;
import org.lovetropics.games.common.core.game.state.statistics.PlayerKey;
import org.lovetropics.games.common.core.game.state.team.GameTeam;
import org.lovetropics.games.common.core.game.state.team.TeamState;

import java.util.Set;
import java.util.stream.Collectors;

public sealed interface GameWinner {
	static GameWinner byPlayerKey(IGamePhase game, PlayerKey key) {
		ServerPlayer player = game.allPlayers().getPlayerBy(key);
		return player != null ? new Player(player) : new OfflinePlayer(key, Component.literal(key.name()));
	}

	Component name();

	ActionSubjects<?> resolveSubjects(IGamePhase game);

	Set<PlayerKey> asPlayerKeys(IGamePhase game);

	record Nobody() implements GameWinner {
		@Override
		public Component name() {
			return MinigameTexts.NOBODY;
		}

		@Override
		public ActionSubjects<?> resolveSubjects(IGamePhase game) {
			return ActionSubjects.EMPTY;
		}

		@Override
		public Set<PlayerKey> asPlayerKeys(IGamePhase game) {
			return Set.of();
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

		@Override
		public Set<PlayerKey> asPlayerKeys(IGamePhase game) {
			return Set.of(PlayerKey.from(player));
		}
	}

	record OfflinePlayer(PlayerKey playerKey, Component name) implements GameWinner {
		@Override
		public ActionSubjects<?> resolveSubjects(IGamePhase game) {
			return ActionSubjects.EMPTY;
		}

		@Override
		public Set<PlayerKey> asPlayerKeys(IGamePhase game) {
			return Set.of(playerKey);
		}
	}

	record Team(GameTeam team) implements GameWinner {
		@Override
		public Component name() {
			return team.styledName();
		}

		@Override
		public ActionSubjects<?> resolveSubjects(IGamePhase game) {
			return ActionSubjects.ofTeam(team.key());
		}

		@Override
		public Set<PlayerKey> asPlayerKeys(IGamePhase game) {
			TeamState teams = game.instanceState().getOrNull(TeamState.KEY);
			if (teams != null) {
				return teams.getPlayersForTeam(game, team.key()).stream().map(PlayerKey::from).collect(Collectors.toSet());
			}
			return Set.of();
		}
	}
}
