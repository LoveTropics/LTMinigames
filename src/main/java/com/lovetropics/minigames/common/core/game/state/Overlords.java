package com.lovetropics.minigames.common.core.game.state;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.player.MutablePlayerSet;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import net.minecraft.server.level.ServerPlayer;

import org.jspecify.annotations.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;

public class Overlords implements PlayerSet, IGameState {
	public static final GameStateKey.Defaulted<Overlords> KEY = new GameStateKey.Defaulted<>("Overlords", Overlords::new);

	private @Nullable MutablePlayerSet players;

	public static Overlords get(IGamePhase game) {
		return game.instanceState().get(Overlords.KEY);
	}

	public void add(ServerPlayer player) {
		if (players == null) {
			players = new MutablePlayerSet(player.level().getServer());
		}
		players.add(player);
	}

	public boolean remove(ServerPlayer player) {
		if (players != null) {
			return players.remove(player);
		}
		return false;
	}

	@Override
	public boolean contains(UUID id) {
		return players != null && players.contains(id);
	}

	@Override
	public @Nullable ServerPlayer getPlayerBy(UUID id) {
		return players != null ? players.getPlayerBy(id) : null;
	}

	@Override
	public Iterator<ServerPlayer> iterator() {
		return players != null ? players.iterator() : Collections.emptyIterator();
	}
}
