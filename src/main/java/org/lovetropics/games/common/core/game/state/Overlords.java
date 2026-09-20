package org.lovetropics.games.common.core.game.state;

import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.player.MutablePlayerSet;
import org.lovetropics.games.common.core.game.player.PlayerSet;

import java.util.Iterator;
import java.util.UUID;

public class Overlords implements PlayerSet, IGameState {
	public static final GameStateKey.Defaulted<Overlords> KEY = new GameStateKey.Defaulted<>("Overlords", Overlords::new);

	private final MutablePlayerSet players = new MutablePlayerSet();

	public static Overlords get(IGamePhase game) {
		return game.instanceState().get(Overlords.KEY);
	}

	public void add(ServerPlayer player) {
		players.add(player);
	}

	public boolean remove(ServerPlayer player) {
			return players.remove(player);
	}

	@Override
	public boolean contains(UUID id) {
		return players.contains(id);
	}

	@Override
	public @Nullable ServerPlayer getPlayerBy(UUID id) {
		return players.getPlayerBy(id);
	}

	@Override
	public Iterator<ServerPlayer> iterator() {
		return players.iterator();
	}
}
