package org.lovetropics.games.common.core.game.map;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import org.apache.commons.lang3.mutable.MutableObject;
import org.lovetropics.dimensions.RuntimeDimensionConfig;
import org.lovetropics.dimensions.RuntimeDimensionHandle;
import org.lovetropics.dimensions.RuntimeDimensions;
import org.lovetropics.dimensions.SharedDimensionState;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.util.world.VoidChunkGenerator;
import org.lovetropics.maps.MapExportReader;
import org.lovetropics.maps.MapMetadata;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public record LoadMapProvider(
		Identifier loadFrom,
		Optional<Holder<DimensionType>> dimensionType
) implements IGameMapProvider {
	public static final MapCodec<LoadMapProvider> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Identifier.CODEC.fieldOf("load_from").forGetter(c -> c.loadFrom),
			DimensionType.CODEC.optionalFieldOf("dimension_type").forGetter(c -> c.dimensionType)
	).apply(i, LoadMapProvider::new));

	@Override
	public MapCodec<LoadMapProvider> getCodec() {
		return CODEC;
	}

	@Override
	public List<ResourceKey<Level>> getPossibleDimensions() {
		return List.of();
	}

	@Override
	public CompletableFuture<GameMap> open(MinecraftServer server) {
		Holder<DimensionType> dimensionType = this.dimensionType.orElse(server.overworld().dimensionTypeRegistration());
		LevelStem dimension = new LevelStem(dimensionType, new VoidChunkGenerator(server));
		SharedDimensionState sharedState = SharedDimensionState.createFresh(server);
		RuntimeDimensionConfig config = new RuntimeDimensionConfig(dimension, sharedState);

		MutableObject<MapMetadata> metadataHolder = new MutableObject<>();
		CompletableFuture<RuntimeDimensionHandle> handleFuture = RuntimeDimensions.get(server).openTemporary(config, dimensionPath ->
				CompletableFuture.runAsync(
						() -> metadataHolder.setValue(loadMapInto(server, dimensionPath)),
						Util.backgroundExecutor()
				)
		);

		return handleFuture.thenApplyAsync(handle -> {
			MapMetadata metadata = metadataHolder.get();
			// The level is already running, so its clocks must be updated from the server thread
			metadata.settings().setupInto(server, sharedState);
			return new GameMap(handle.asKey(), metadata.regions())
					.onClose(_ -> handle.markForDeletion());
		}, server);
	}

	private MapMetadata loadMapInto(MinecraftServer server, Path dimensionPath) {
		try {
			try (MapExportReader reader = MapExportReader.open(server, loadFrom)) {
				return reader.loadInto(dimensionPath);
			}
		} catch (IOException e) {
			throw new GameException(Component.literal("Failed to load map with id '" + loadFrom + "'" + e.getMessage()), e);
		}
	}
}
