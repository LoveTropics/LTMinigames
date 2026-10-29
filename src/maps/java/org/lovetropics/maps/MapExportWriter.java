package org.lovetropics.maps;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.neoforged.neoforge.attachment.LevelAttachmentsSavedData;

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
import java.util.Set;

public final class MapExportWriter implements Closeable {
	private static final Set<Identifier> INCLUDE_SAVED_DATA = Set.of(
			LevelAttachmentsSavedData.TYPE.id()
	);

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
		CompoundTag nbt = (CompoundTag) MapMetadata.CODEC.encodeStart(NbtOps.INSTANCE, metadata).getOrThrow();

		Path path = fs.getPath("metadata.nbt");
		try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path)))) {
			NbtIo.write(nbt, output);
		}
	}

	public void writeWorldData(Path dimension) throws IOException {
		copyDirectory(dimension.resolve("region"), fs.getPath("world"));
		copyDirectory(dimension.resolve("entities"), fs.getPath("entities"));
		for (Identifier id : INCLUDE_SAVED_DATA) {
			Identifier file = id.withSuffix(".dat");
			copyFileIfExists(file.resolveAgainst(dimension.resolve("data")), file.resolveAgainst(fs.getPath("data")));
		}
	}

	private static void copyDirectory(Path sourceRoot, Path targetRoot) throws IOException {
		Files.walkFileTree(sourceRoot, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path source, BasicFileAttributes attrs) throws IOException {
				Path target = targetRoot.resolve(sourceRoot.relativize(source).toString());
				copyFile(source, target);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private static void copyFileIfExists(Path source, Path target) throws IOException {
		if (Files.exists(source)) {
			copyFile(source, target);
		}
	}

	private static void copyFile(Path source, Path target) throws IOException {
		Files.createDirectories(target.getParent());
		Files.copy(source, target);
	}

	@Override
	public void close() throws IOException {
		fs.close();
	}
}
