package com.lovetropics.minigames.common.core.game.player;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueOutput;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * In-memory storage of player data, useful when we want to store player data but not on disk
 */
public class PlayerStorage {
	private static final Logger LOGGER = LogUtils.getLogger();

	private final Object2ObjectMap<UUID, CompoundTag> storage = new Object2ObjectOpenHashMap<>();

	public Optional<CompoundTag> takePlayerData(final UUID playerId) {
		CompoundTag remove = storage.remove(playerId);
		return Optional.ofNullable(remove);
	}

	public void store(final ServerPlayer player) {
		try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(player.problemPath(), LOGGER)) {
			TagValueOutput output = TagValueOutput.createWithContext(reporter, player.registryAccess());
			player.saveWithoutId(output);
			storage.put(player.getUUID(), output.buildResult());
		}
	}
}
