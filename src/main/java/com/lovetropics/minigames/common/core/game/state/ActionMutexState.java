package com.lovetropics.minigames.common.core.game.state;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import org.jspecify.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ActionMutexState implements IGameState {
	public static final GameStateKey.Defaulted<ActionMutexState> KEY = GameStateKey.create("Action mutexes", ActionMutexState::new);

	private final Map<Identifier, MutexMap> mutexMaps = new HashMap<>();

	private MutexMap getMutexMap(Identifier id) {
		return mutexMaps.computeIfAbsent(id, k -> new MutexMap());
	}

	public @Nullable ActionMutex acquireGlobal(Identifier id, boolean force) {
		return getMutexMap(id).acquireGlobal(force);
	}

	public @Nullable ActionMutex acquireForPlayer(ServerPlayer player, Identifier id, boolean force) {
		return getMutexMap(id).acquireForPlayer(player, force);
	}

	/* package-private */ static class MutexMap {
		private @Nullable ActionMutex global;
		private final Map<UUID, ActionMutex> byPlayer = new HashMap<>();

		public @Nullable ActionMutex acquireGlobal(boolean force) {
			if (force) {
				ActionMutex oldMutex = global;
				global = new ActionMutex(this, null);
				if (oldMutex != null) {
					oldMutex.close();
				}
				return global;
			} else {
				if (global == null) {
					global = new ActionMutex(this, null);
					return global;
				}
				return null;
			}
		}

		public @Nullable ActionMutex acquireForPlayer(ServerPlayer player, boolean force) {
			ActionMutex newMutex = new ActionMutex(this, player.getUUID());
			if (force) {
				ActionMutex oldMutex = byPlayer.put(player.getUUID(), newMutex);
				if (oldMutex != null) {
					oldMutex.close();
				}
				return newMutex;
			} else {
				if (byPlayer.putIfAbsent(player.getUUID(), newMutex) != null) {
					return null;
				}
				return newMutex;
			}
		}

		public void release(ActionMutex mutex) {
			UUID playerId = mutex.playerId();
			if (playerId != null) {
				byPlayer.remove(playerId, mutex);
			} else if (global == mutex) {
				global = null;
			}
		}
	}
}
