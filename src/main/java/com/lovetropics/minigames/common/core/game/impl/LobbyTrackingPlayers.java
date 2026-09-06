package com.lovetropics.minigames.common.core.game.impl;

import com.lovetropics.minigames.common.core.game.player.MutablePlayerSet;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import net.minecraft.server.level.ServerPlayer;

import org.jspecify.annotations.Nullable;
import java.util.Iterator;
import java.util.UUID;

final class LobbyTrackingPlayers implements PlayerSet {
	private final GameLobby lobby;
	private final MutablePlayerSet tracking;

	LobbyTrackingPlayers(GameLobby lobby) {
		this.lobby = lobby;
		tracking = new MutablePlayerSet(lobby.getServer());

		rebuildTracking();
	}

	void rebuildTracking() {
		for (ServerPlayer player : PlayerSet.ofServer(lobby.getServer())) {
			if (lobby.isVisibleTo(player)) {
				startTracking(player);
			} else {
				stopTracking(player);
			}
		}
	}

	void onPlayerLoggedIn(ServerPlayer player) {
		if (lobby.isVisibleTo(player)) {
			startTracking(player);
		}
	}

	void onPlayerLoggedOut(ServerPlayer player) {
		stopTracking(player);
	}

	private void startTracking(ServerPlayer player) {
		if (tracking.add(player)) {
			lobby.onPlayerStartTracking(player);
		}
	}

	private void stopTracking(ServerPlayer player) {
		if (tracking.remove(player)) {
			lobby.onPlayerStopTracking(player);
		}
	}

	@Override
	public boolean contains(UUID id) {
		return tracking.contains(id);
	}

	@Override
	public @Nullable ServerPlayer getPlayerBy(UUID id) {
		return tracking.getPlayerBy(id);
	}

	@Override
	public int size() {
		return tracking.size();
	}

	@Override
	public Iterator<ServerPlayer> iterator() {
		return tracking.iterator();
	}
}
