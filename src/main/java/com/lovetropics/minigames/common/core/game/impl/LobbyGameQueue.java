package com.lovetropics.minigames.common.core.game.impl;

import com.lovetropics.minigames.common.core.game.IGameDefinition;
import com.lovetropics.minigames.common.core.game.lobby.ILobbyGameQueue;
import com.lovetropics.minigames.common.core.game.lobby.QueuedGame;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class LobbyGameQueue implements ILobbyGameQueue {
	private final MinecraftServer server;
	private final List<QueuedGame> entries = new ArrayList<>();

	LobbyGameQueue(MinecraftServer server) {
		this.server = server;
	}

	@Nullable
	QueuedGame next() {
		return !entries.isEmpty() ? entries.removeFirst() : null;
	}

	@Override
	public QueuedGame enqueue(IGameDefinition game) {
		QueuedGame entry = QueuedGame.create(game);
		entries.add(entry);
		return entry;
	}

	@Override
	public void clear() {
		entries.clear();
	}

	@Override
	public Iterator<QueuedGame> iterator() {
		return entries.iterator();
	}

	@Override
	public int size() {
		return entries.size();
	}

	@Nullable
	QueuedGame removeByNetworkId(int networkId) {
		int index = indexByNetworkId(networkId);
		return index != -1 ? entries.remove(index) : null;
	}

	boolean reorderByNetworkId(int networkId, int newIndex) {
		int index = indexByNetworkId(networkId);
		if (index == -1 || index == newIndex) {
			return false;
		}

		QueuedGame entry = entries.remove(index);
		entries.add(Mth.clamp(newIndex, 0, entries.size()), entry);

		return true;
	}

	@Nullable
	QueuedGame getByNetworkId(int networkId) {
		int index = indexByNetworkId(networkId);
		return index != -1 ? entries.get(index) : null;
	}

	int indexByNetworkId(int networkId) {
		List<QueuedGame> entries = this.entries;
		for (int index = 0; index < entries.size(); index++) {
			if (entries.get(index).networkId() == networkId) {
				return index;
			}
		}
		return -1;
	}
}
