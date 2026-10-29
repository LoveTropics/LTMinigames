package org.lovetropics.maps;

import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Util;
import org.apache.commons.io.FileUtils;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class MapExportReader implements Closeable {
	private static final FileToIdConverter ID_CONVERTER = new FileToIdConverter("maps", ".zip");

	private static volatile @Nullable ListCache listCache;

	private final ZipInputStream input;

	private MapExportReader(ZipInputStream input) {
		this.input = input;
	}

	public static CompletableFuture<Stream<Identifier>> list(ResourceManager resourceManager) {
		// Extremely naive cache, it doesn't matter if we load multiple times
		ListCache cache = listCache;
		if (cache != null && cache.resourceManager.refersTo(resourceManager)) {
			return CompletableFuture.completedFuture(cache.ids.stream());
		}
		return CompletableFuture.supplyAsync(() -> {
			Set<Identifier> ids = ID_CONVERTER.listMatchingResources(resourceManager).keySet().stream()
					.map(ID_CONVERTER::fileToId)
					.collect(Collectors.toSet());
			listCache = new ListCache(new WeakReference<>(resourceManager), ids);
			return ids.stream();
		}, Util.nonCriticalIoPool());
	}

	public static boolean exists(MinecraftServer server, Identifier location) {
		Optional<Resource> resource = server.getResourceManager().getResource(ID_CONVERTER.idToFile(location));
		return resource.isPresent();
	}

	public static MapExportReader open(MinecraftServer server, Identifier location) throws IOException {
		Identifier path = ID_CONVERTER.idToFile(location);
		Optional<Resource> resource = server.getResourceManager().getResource(path);
		if (resource.isEmpty()) {
			throw new IOException("Map at " + location + " did not exist");
		}
		return MapExportReader.open(resource.get().open());
	}

	public static MapExportReader open(InputStream input) {
		return new MapExportReader(new ZipInputStream(input));
	}

	public MapMetadata loadInto(Path dimensionRoot) throws IOException {
		Path regionRoot = dimensionRoot.resolve("region");
		Path entitiesRoot = dimensionRoot.resolve("entities");
		Path savedDataRoot = dimensionRoot.resolve("data");
		createClearDirectories(regionRoot);
		createClearDirectories(entitiesRoot);
		createClearDirectories(savedDataRoot);

		MapMetadata metadata = null;

		ZipEntry entry;
		while ((entry = input.getNextEntry()) != null) {
			if (entry.isDirectory()) {
				continue;
			}

			String name = entry.getName();
			if (name.equals("metadata.nbt")) {
				metadata = MapMetadata.CODEC.parse(NbtOps.INSTANCE, NbtIo.read(new DataInputStream(input))).getOrThrow(IOException::new);
			} else if (name.startsWith("world/")) {
				Path targetPath = regionRoot.resolve(name.substring("world/".length()));
				Files.copy(input, targetPath);
			} else if (name.startsWith("entities/")) {
				Path targetPath = entitiesRoot.resolve(name.substring("entities/".length()));
				Files.copy(input, targetPath);
			} else if (name.startsWith("data/")) {
				Path targetPath = savedDataRoot.resolve(name.substring("data/".length()));
				Files.createDirectories(targetPath.getParent());
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

	private record ListCache(
			WeakReference<ResourceManager> resourceManager,
			Set<Identifier> ids
	) {
	}
}
