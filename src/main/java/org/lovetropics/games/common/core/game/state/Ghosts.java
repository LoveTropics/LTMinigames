package org.lovetropics.games.common.core.game.state;

import net.minecraft.server.level.ServerPlayer;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.player.PlayerRole;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/// Spectators who can still act on the players in the game (usually because they were playing themselves.
public class Ghosts implements IGameState {
	public static final GameStateKey.Defaulted<Ghosts> KEY = GameStateKey.create("Ghosts", Ghosts::new);

	private final Set<UUID> players = new HashSet<>();

	public static Ghosts get(IGamePhase game) {
		return game.instanceState().get(Ghosts.KEY);
	}

	public static boolean isParticipantOrGhost(IGamePhase game, ServerPlayer player) {
		return game.getRoleFor(player) == PlayerRole.PARTICIPANT || get(game).contains(player);
	}

	public void add(ServerPlayer player) {
		players.add(player.getUUID());
	}

	public void remove(ServerPlayer player) {
		players.remove(player.getUUID());
	}

	public boolean contains(ServerPlayer player) {
		return players.contains(player.getUUID());
	}
}
