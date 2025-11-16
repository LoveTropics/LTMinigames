package com.lovetropics.minigames.common.core.game.state;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ActionMutexState implements IGameState {
	public static final GameStateKey.Defaulted<ActionMutexState> KEY = GameStateKey.create("Action mutexes", ActionMutexState::new);

	private final Map<ResourceLocation, MutexMap> mutexMaps = new HashMap<>();

	private MutexMap getMutexMap(ResourceLocation id) {
		return mutexMaps.computeIfAbsent(id, k -> new MutexMap());
	}

	@Nullable
	public ActionMutex acquireGlobal(ResourceLocation id, boolean force) {
		return getMutexMap(id).acquireGlobal(force);
	}

	@Nullable
	public ActionMutex acquireForPlayer(ServerPlayer player, ResourceLocation id, boolean force) {
		return getMutexMap(id).acquireForPlayer(player, force);
	}

	/* package-private */ static class MutexMap {
		@Nullable
		private ActionMutex global;
		private final Map<UUID, ActionMutex> byPlayer = new HashMap<>();

		@Nullable
		public ActionMutex acquireGlobal(boolean force) {
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

		@Nullable
		public ActionMutex acquireForPlayer(ServerPlayer player, boolean force) {
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
