package com.lovetropics.minigames.common.core.game.impl;

import com.lovetropics.minigames.common.core.game.IGameDefinition;
import com.lovetropics.minigames.common.core.game.config.GameConfig;
import com.lovetropics.minigames.common.core.game.lobby.QueuedGame;
import com.lovetropics.minigames.common.dev.DevQuickPlay;
import net.minecraft.util.Mth;

import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class LobbyGameQueue implements Iterable<QueuedGame> {
	private final List<QueuedGame> entries = new ArrayList<>();

	@Nullable QueuedGame next() {
		if (!entries.isEmpty()) {
			return entries.removeFirst();
		}
		GameConfig quickPlayGame = DevQuickPlay.getQuickPlayGame();
		if (quickPlayGame != null) {
			return QueuedGame.create(quickPlayGame);
		}
		return null;
	}

	public QueuedGame enqueue(IGameDefinition game) {
		QueuedGame entry = QueuedGame.create(game);
		entries.add(entry);
		return entry;
	}

	public void clear() {
		entries.clear();
	}

	@Override
	public Iterator<QueuedGame> iterator() {
		return entries.iterator();
	}

	public int size() {
		return entries.size();
	}

	@Nullable QueuedGame removeByNetworkId(int networkId) {
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

	@Nullable QueuedGame getByNetworkId(int networkId) {
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
