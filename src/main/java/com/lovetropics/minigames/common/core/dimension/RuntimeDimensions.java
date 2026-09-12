package com.lovetropics.minigames.common.core.dimension;

import com.lovetropics.minigames.LoveTropics;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProgressListener;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.dimension.LevelStem;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@EventBusSubscriber(modid = LoveTropics.ID)
public final class RuntimeDimensions {
	private static final Logger LOGGER = LogManager.getLogger(RuntimeDimensions.class);

	private static @Nullable RuntimeDimensions instance;

	private final MinecraftServer server;

	private final Set<ServerLevel> deletionQueue = new ReferenceOpenHashSet<>();
	private final Set<ResourceKey<Level>> temporaryDimensions = new ReferenceOpenHashSet<>();
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
		ResourceKey<Level> worldKey = ResourceKey.create(Registries.DIMENSION, key);
		ServerLevel world = server.getLevel(worldKey);
		if (world != null) {
			deletionQueue.remove(world);
			return new RuntimeDimensionHandle(this, world);
		}

		return openLevel(key, config.get(), false);
	}

	public RuntimeDimensionHandle openTemporary(RuntimeDimensionConfig config) {
		Identifier key = generateTemporaryDimensionKey();
		return openLevel(key, config, true);
	}

	public @Nullable RuntimeDimensionHandle openTemporaryWithKey(Identifier key, RuntimeDimensionConfig config) {
		ResourceKey<Level> worldKey = ResourceKey.create(Registries.DIMENSION, key);
		if (server.getLevel(worldKey) == null) {
			return openLevel(key, config, true);
		} else {
			return null;
		}
	}

	private RuntimeDimensionHandle openLevel(Identifier key, RuntimeDimensionConfig config, boolean temporary) {
		ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, key);

		MappedRegistry<LevelStem> dimensionsRegistry = getLevelStemRegistry(server);
		dimensionsRegistry.unfreeze(false);
		dimensionsRegistry.register(ResourceKey.create(Registries.LEVEL_STEM, key), config.dimension(), RegistrationInfo.BUILT_IN);
		dimensionsRegistry.freeze();

		ServerLevel level = new ServerLevel(
				server,
				Util.backgroundExecutor(),
				server.storageSource,
				config.worldInfo(),
				levelKey,
				config.dimension(),
				false,
				BiomeManager.obfuscateSeed(config.seed()),
				List.of(),
				false) {
			@Override
			public void save(@Nullable ProgressListener progress, boolean flush, boolean skipSave) {
				if (temporary) {
					try {
						if (!flush && temporaryDimensions.contains(dimension())) {
							super.save(progress, false, skipSave);
						}
					} catch (final Exception e) {
						LOGGER.error("Failed to save temporary dimension", e);
					}
				} else {
					super.save(progress, flush, skipSave);
				}
			}
		};

		server.levels.put(levelKey, level);
		server.markWorldsDirty();

		if (temporary) {
			temporaryDimensions.add(levelKey);
		}

		NeoForge.EVENT_BUS.post(new LevelEvent.Load(level));

		level.tick(() -> true);

		return new RuntimeDimensionHandle(this, level);
	}

	void tick() {
		if (!deletionQueue.isEmpty()) {
			deletionQueue.removeIf(this::tickDimensionDeletion);
		}
	}

	boolean tickDimensionDeletion(ServerLevel world) {
		prepareForDeletion(world);
		if (isWorldUnloaded(world) || isTemporaryDimension(world.dimension())) {
			deleteDimension(world);
			return true;
		} else {
			return false;
		}
	}

	private void stop() {
		ArrayList<ResourceKey<Level>> temporaryDimensions = new ArrayList<>(this.temporaryDimensions);
		for (ResourceKey<Level> dimension : temporaryDimensions) {
			ServerLevel world = server.getLevel(dimension);
			if (world != null) {
				prepareForDeletion(world);
				deleteDimension(world);
			}
		}
	}

	void enqueueDeletion(ServerLevel world) {
		CompletableFuture.runAsync(() -> {
			deletionQueue.add(world);
		}, server);
	}

	private void prepareForDeletion(ServerLevel level) {
		LongSet forceLoadedChunks = new LongOpenHashSet(level.getChunkSource().getForceLoadedChunks());
		forceLoadedChunks.forEach(chunkKey ->
				level.getChunkSource().updateChunkForced(ChunkPos.unpack(chunkKey), false)
		);
		kickPlayersFrom(level);
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

	private boolean isWorldUnloaded(ServerLevel world) {
		return world.players().isEmpty() && world.getChunkSource().getLoadedChunksCount() <= 0;
	}

	private void deleteDimension(ServerLevel level) {
		ResourceKey<Level> dimensionKey = level.dimension();

		if (server.levels.remove(dimensionKey, level)) {
			server.markWorldsDirty();

			temporaryDimensions.remove(dimensionKey);
			links.remove(dimensionKey);

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
		String random = RandomStringUtils.random(16, "abcdefghijklmnopqrstuvwxyz0123456789");
		return LoveTropics.id("tmp_" + random);
	}

	public boolean isTemporaryDimension(ResourceKey<Level> dimension) {
		return temporaryDimensions.contains(dimension);
	}

	public RuntimeDimensionHandle handleForTemporaryDimension(ResourceKey<Level> dimension) {
		if (!isTemporaryDimension(dimension)) {
			throw new IllegalArgumentException("must be a temporary dimension");
		}
		return new RuntimeDimensionHandle(this, server.getLevel(dimension));
	}

	public Collection<ResourceKey<Level>> getTemporaryDimensions() {
		return temporaryDimensions;
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

	public static boolean isTemporaryDimension(ServerLevel level) {
		RuntimeDimensions instance = getOrNull(level.getServer());
		return instance != null && instance.isTemporaryDimension(level.dimension());
	}
}
