package com.lovetropics.minigames.common.core.map;

import net.minecraft.util.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

public final class MapExportWriter implements Closeable {
	private final FileSystem fs;

	private MapExportWriter(FileSystem fs) {
		this.fs = fs;
	}

	public static MapExportWriter open(Path path) throws IOException {
		Files.deleteIfExists(path);
		return new MapExportWriter(Util.ZIP_FILE_SYSTEM_PROVIDER.newFileSystem(path, Map.of("create", "true")));
	}

	public static Path pathFor(Identifier id) {
		return Paths.get("export", id.getNamespace(), "maps", id.getPath() + ".zip");
	}

	public void writeMetadata(MapMetadata metadata) throws IOException {
		CompoundTag nbt = metadata.write();

		Path path = fs.getPath("metadata.nbt");
		try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path)))) {
			NbtIo.write(nbt, output);
		}
	}

	public void writeWorldData(Path sourceRoot) throws IOException {
		copyDirectory(sourceRoot.resolve("region"), fs.getPath("world"));
		copyDirectory(sourceRoot.resolve("entities"), fs.getPath("entities"));
	}

	private static void copyDirectory(Path sourceRoot, Path targetRoot) throws IOException {
		Files.walkFileTree(sourceRoot, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path source, BasicFileAttributes attrs) throws IOException {
				Path target = targetRoot.resolve(sourceRoot.relativize(source).toString());
				Files.createDirectories(target.getParent());
				Files.copy(source, target);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	@Override
	public void close() throws IOException {
		fs.close();
	}
}
