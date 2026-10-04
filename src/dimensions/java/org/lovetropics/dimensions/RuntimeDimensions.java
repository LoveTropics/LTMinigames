package org.lovetropics.dimensions;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.jspecify.annotations.Nullable;
import org.lovetropics.dimensions.duck.RegistryEntryRemover;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@EventBusSubscriber(modid = LTDimensionsMod.ID)
public final class RuntimeDimensions {
	private static final Logger LOGGER = LogUtils.getLogger();

	private static @Nullable RuntimeDimensions instance;

	private final MinecraftServer server;

	private final Map<ResourceKey<Level>, LinkedDimensions> links = new ConcurrentHashMap<>();

	private RuntimeDimensions(MinecraftServer server) {
		this.server = server;
	}

	@SubscribeEvent
	public static void onServerAboutToStart(ServerAboutToStartEvent event) {
		instance = new RuntimeDimensions(event.getServer());
	}

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Pre event) {
		RuntimeDimensions instance = RuntimeDimensions.instance;
		if (instance != null) {
			instance.tick();
		}
	}

	@SubscribeEvent
	public static void onServerStopping(ServerStoppingEvent event) {
		RuntimeDimensions.tryStop();
	}

	public static void onServerStoppingUnsafely(MinecraftServer server) {
		RuntimeDimensions.tryStop();
	}

	private static void tryStop() {
		RuntimeDimensions instance = RuntimeDimensions.instance;
		if (instance != null) {
			instance.stop();
			RuntimeDimensions.instance = null;
		}
	}

	public static RuntimeDimensions get(MinecraftServer server) {
		return Objects.requireNonNull(getOrNull(server), "Runtime dimensions not yet initialized");
	}

	public static @Nullable RuntimeDimensions getOrNull(MinecraftServer server) {
		RuntimeDimensions instance = RuntimeDimensions.instance;
		if (instance != null && instance.server == server) {
			return instance;
		}
		return null;
	}

	public RuntimeDimensionHandle getOrOpenPersistent(Identifier key, Supplier<RuntimeDimensionConfig> config) {
		ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, key);
		if (server.getLevel(dimension) instanceof RuntimeServerLevel existingLevel) {
			// If the dimension wasn't ready to delete yet anyway, just revive it
			existingLevel.handle().revive();
			return existingLevel.handle();
		}

		return openLevel(key, config.get(), false);
	}

	public RuntimeDimensionHandle openTemporary(RuntimeDimensionConfig config) {
		Identifier key = generateTemporaryDimensionKey();
		return openLevel(key, config, true);
	}

	private RuntimeDimensionHandle openLevel(Identifier key, RuntimeDimensionConfig config, boolean temporary) {
		ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, key);

		MappedRegistry<LevelStem> dimensionsRegistry = getLevelStemRegistry(server);
		dimensionsRegistry.unfreeze(false);
		dimensionsRegistry.register(ResourceKey.create(Registries.LEVEL_STEM, key), config.levelStem(), RegistrationInfo.BUILT_IN);
		dimensionsRegistry.freeze();

		RuntimeServerLevel level = new RuntimeServerLevel(server, levelKey, config, temporary);

		server.levels.put(levelKey, level);
		server.markWorldsDirty();

		NeoForge.EVENT_BUS.post(new LevelEvent.Load(level));

		level.tick(() -> true);

		return level.handle();
	}

	void tick() {
		List<RuntimeServerLevel> levelsToDelete = null;
		for (ServerLevel level : server.getAllLevels()) {
			if (level instanceof RuntimeServerLevel runtimeLevel && !runtimeLevel.handle().isValid()) {
				if (prepareForDeletion(runtimeLevel)) {
					if (levelsToDelete == null) {
						levelsToDelete = new ArrayList<>();
					}
					levelsToDelete.add(runtimeLevel);
				}
			}
		}
		if (levelsToDelete != null) {
			for (RuntimeServerLevel level : levelsToDelete) {
				deleteDimension(level);
			}
		}
	}

	private void stop() {
		List<RuntimeServerLevel> levelsToDelete = new ArrayList<>();
		for (ServerLevel level : server.getAllLevels()) {
			if (level instanceof RuntimeServerLevel runtimeLevel && runtimeLevel.isTemporary()) {
				levelsToDelete.add(runtimeLevel);
				// Ignore whether we consider this level ready for graceful deletion - the server is closing anyway
				prepareForDeletion(runtimeLevel);
			}
		}
		for (RuntimeServerLevel level : levelsToDelete) {
			deleteDimension(level);
		}
	}

	private boolean prepareForDeletion(RuntimeServerLevel level) {
		LongSet forceLoadedChunks = new LongOpenHashSet(level.getChunkSource().getForceLoadedChunks());
		forceLoadedChunks.forEach(chunkKey ->
				level.getChunkSource().updateChunkForced(ChunkPos.unpack(chunkKey), false)
		);
		kickPlayersFrom(level);
		return isLevelUnloaded(level) || level.isTemporary();
	}

	private void kickPlayersFrom(ServerLevel level) {
		if (level.players().isEmpty()) {
			return;
		}

		ServerLevel overworld = server.overworld();
		BlockPos spawnPos = overworld.getRespawnData().pos();
		float spawnAngleYaw = overworld.getRespawnData().yaw();
		float spawnAnglePitch = overworld.getRespawnData().pitch();

		List<ServerPlayer> players = new ArrayList<>(level.players());
		for (ServerPlayer player : players) {
			player.teleportTo(overworld, spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, Set.of(), spawnAngleYaw, spawnAnglePitch, true);
		}
	}

	private boolean isLevelUnloaded(ServerLevel level) {
		return level.players().isEmpty() && level.getChunkSource().getLoadedChunksCount() <= 0;
	}

	private void deleteDimension(RuntimeServerLevel level) {
		ResourceKey<Level> dimensionKey = level.dimension();

		if (server.levels.remove(dimensionKey, level)) {
			server.markWorldsDirty();

			// If this is normal server stop, make sure the handle is reported as invalid
			level.handle().markForDeletion();

			links.remove(dimensionKey);

			// The dragon fight only drops players from its boss bar while its level ticks, so do it before it's gone for good
			EnderDragonFight dragonFight = level.getDragonFight();
			if (dragonFight != null) {
				server.getPlayerList().getPlayers().forEach(dragonFight::removePlayer);
			}

			NeoForge.EVENT_BUS.post(new LevelEvent.Unload(level));

			MappedRegistry<LevelStem> dimensionsRegistry = getLevelStemRegistry(server);

			dimensionsRegistry.unfreeze(false);
			RegistryEntryRemover.remove(dimensionsRegistry, dimensionKey.identifier());
			dimensionsRegistry.freeze();

			LevelStorageSource.LevelStorageAccess save = server.storageSource;
			Path dimensionPath = save.getDimensionPath(dimensionKey);
			Util.ioPool().execute(() -> {
				try {
					level.close();
				} catch (IOException e) {
					LOGGER.error("Failed to close runtime level", e);
				}
				deleteWorldDirectory(dimensionPath);
			});
		}
	}

	private static void deleteWorldDirectory(Path worldDirectory) {
		if (Files.exists(worldDirectory)) {
			try {
				FileUtils.deleteDirectory(worldDirectory.toFile());
			} catch (IOException e) {
				LOGGER.warn("Failed to delete world directory", e);
				try {
					FileUtils.forceDeleteOnExit(worldDirectory.toFile());
				} catch (IOException ignored) {
				}
			}
		}
	}

	private static MappedRegistry<LevelStem> getLevelStemRegistry(MinecraftServer server) {
		return (MappedRegistry<LevelStem>) server.registryAccess().lookupOrThrow(Registries.LEVEL_STEM);
	}

	private static Identifier generateTemporaryDimensionKey() {
		String random = RandomStringUtils.insecure().next(16, "abcdefghijklmnopqrstuvwxyz0123456789");
		return LTDimensionsMod.id("tmp_" + random);
	}

	public boolean isTemporaryDimension(ServerLevel level) {
		return level instanceof RuntimeServerLevel runtimeLevel && runtimeLevel.isTemporary();
	}

	public boolean isTemporaryDimension(ResourceKey<Level> dimension) {
		ServerLevel level = server.getLevel(dimension);
		return level != null && isTemporaryDimension(level);
	}

	/* package-private */ @Nullable RuntimeDimensionHandle asHandle(ServerLevel level) {
		return level instanceof RuntimeServerLevel runtimeLevel ? runtimeLevel.handle() : null;
	}

	/// Makes the given dimensions stand in for the vanilla Overworld, Nether and End for each other, until they are deleted
	public void link(LinkedDimensions linkedDimensions) {
		for (ResourceKey<Level> dimension : linkedDimensions.dimensions()) {
			links.put(dimension, linkedDimensions);
		}
	}

	public @Nullable LinkedDimensions getLinks(ResourceKey<Level> dimension) {
		return links.get(dimension);
	}
}
