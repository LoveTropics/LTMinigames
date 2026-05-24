package com.lovetropics.minigames.common.core.map;

import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.apache.commons.io.FileUtils;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class MapExportReader implements Closeable {
	private final ZipInputStream input;

	private MapExportReader(ZipInputStream input) {
		this.input = input;
	}

	public static boolean exists(MinecraftServer server, Identifier location) {
		Identifier path = location.withPath(p -> "maps/" + p + ".zip");
		Optional<Resource> resource = server.getResourceManager().getResource(path);
		return resource.isPresent();
	}

	public static MapExportReader open(MinecraftServer server, Identifier location) throws IOException {
		Identifier path = location.withPath(p -> "maps/" + p + ".zip");
		Optional<Resource> resource = server.getResourceManager().getResource(path);
		if (resource.isEmpty()) {
			throw new IOException("Map at " + location + " did not exist");
		}
		return MapExportReader.open(resource.get().open());
	}

	public static MapExportReader open(InputStream input) {
		return new MapExportReader(new ZipInputStream(input));
	}

	public MapMetadata loadInto(MinecraftServer server, ResourceKey<Level> dimension) throws IOException {
		LevelStorageSource.LevelStorageAccess save = server.storageSource;
		return loadInto(save.getDimensionPath(dimension));
	}

	public MapMetadata loadInto(Path dimensionRoot) throws IOException {
		Path regionRoot = dimensionRoot.resolve("region");
		Path entitiesRoot = dimensionRoot.resolve("entities");
		createClearDirectories(regionRoot);
		createClearDirectories(entitiesRoot);

		MapMetadata metadata = null;

		ZipEntry entry;
		while ((entry = input.getNextEntry()) != null) {
			if (entry.isDirectory()) {
				continue;
			}

			String name = entry.getName();
			if (name.equals("metadata.nbt")) {
				metadata = MapMetadata.read(NbtIo.read(new DataInputStream(input)));
			} else if (name.startsWith("world/")) {
				Path targetPath = regionRoot.resolve(name.substring("world/".length()));
				Files.copy(input, targetPath);
			} else if (name.startsWith("entities")) {
				Path targetPath = entitiesRoot.resolve(name.substring("entities/".length()));
				Files.copy(input, targetPath);
			}
		}

		if (metadata == null) {
			throw new IOException("missing metadata");
		}

		return metadata;
	}

	private static void createClearDirectories(Path path) throws IOException {
		if (Files.exists(path)) {
			FileUtils.deleteDirectory(path.toFile());
		}
		Files.createDirectories(path);
	}

	@Override
	public void close() throws IOException {
		input.close();
	}
}
