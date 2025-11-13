package com.lovetropics.minigames.common.core.game;

import com.lovetropics.minigames.common.content.MinigameTexts;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.state.statistics.PlayerKey;
import com.lovetropics.minigames.common.core.game.state.team.GameTeam;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public sealed interface GameWinner {
	static GameWinner byPlayerKey(IGamePhase game, PlayerKey key) {
		ServerPlayer player = game.allPlayers().getPlayerBy(key);
		return player != null ? new Player(player) : new OfflinePlayer(key, Component.literal(key.name()));
	}

	Component name();

	PlayerSet resolvePlayers(IGamePhase game);

	record Nobody() implements GameWinner {
		@Override
		public Component name() {
			return MinigameTexts.NOBODY;
		}

		@Override
		public PlayerSet resolvePlayers(IGamePhase game) {
			return PlayerSet.EMPTY;
		}
	}

	record Player(ServerPlayer player) implements GameWinner {
		@Override
		public Component name() {
			return player.getDisplayName();
		}

		@Override
		public PlayerSet resolvePlayers(IGamePhase game) {
			return PlayerSet.of(player);
		}
	}

	record OfflinePlayer(PlayerKey playerKey, Component name) implements GameWinner {
		@Override
		public PlayerSet resolvePlayers(IGamePhase game) {
			return PlayerSet.EMPTY;
		}
	}

	record Team(GameTeam team) implements GameWinner {
		@Override
		public Component name() {
			return team.config().styledName();
		}

		@Override
		public PlayerSet resolvePlayers(IGamePhase game) {
			return game.instanceState().getOrThrow(TeamState.KEY).getPlayersForTeam(game, team.key());
		}
	}
}
