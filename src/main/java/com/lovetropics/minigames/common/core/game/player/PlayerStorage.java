package com.lovetropics.minigames.common.core.game.player;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * In-memory storage of player data, useful when we want to store player data but not on disk
 */
public class PlayerStorage {
	private final Object2ObjectMap<UUID, CompoundTag> storage = new Object2ObjectOpenHashMap<>();

	public Optional<CompoundTag> fetchAndRemovePlayerData(final UUID playerId) {
		@Nullable final CompoundTag compoundTag = storage.get(playerId);
		if (compoundTag != null) {
			storage.remove(playerId);
			return Optional.of(compoundTag);
		}
		return Optional.empty();
	}

	public void setPlayerData(final ServerPlayer player, final CompoundTag tag) {
		storage.put(player.getUUID(), tag);
	}
}
