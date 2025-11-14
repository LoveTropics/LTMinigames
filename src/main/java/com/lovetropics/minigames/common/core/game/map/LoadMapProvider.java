package com.lovetropics.minigames.common.core.game.map;

import com.lovetropics.minigames.common.core.dimension.RuntimeDimensionConfig;
import com.lovetropics.minigames.common.core.dimension.RuntimeDimensionHandle;
import com.lovetropics.minigames.common.core.dimension.RuntimeDimensions;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.map.MapExportReader;
import com.lovetropics.minigames.common.core.map.MapMetadata;
import com.lovetropics.minigames.common.core.map.MapWorldInfo;
import com.lovetropics.minigames.common.core.map.MapWorldSettings;
import com.lovetropics.minigames.common.core.map.VoidChunkGenerator;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public record LoadMapProvider(
		Optional<String> name,
		ResourceLocation loadFrom,
		Optional<Holder<DimensionType>> dimensionType,
		Optional<ResourceLocation> dimension
) implements IGameMapProvider {
	public static final MapCodec<LoadMapProvider> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.optionalFieldOf("name").forGetter(c -> c.name),
			ResourceLocation.CODEC.fieldOf("load_from").forGetter(c -> c.loadFrom),
			DimensionType.CODEC.optionalFieldOf("dimension_type").forGetter(c -> c.dimensionType),
			ResourceLocation.CODEC.optionalFieldOf("dimension").forGetter(c -> c.dimension)
	).apply(i, LoadMapProvider::new));

	private static final Logger LOGGER = LogManager.getLogger(LoadMapProvider.class);

	@Override
	public MapCodec<LoadMapProvider> getCodec() {
		return CODEC;
	}

	@Override
	public List<ResourceKey<Level>> getPossibleDimensions() {
		return dimension.map(location -> List.of(ResourceKey.create(Registries.DIMENSION, location))).orElseGet(List::of);
	}

	@Override
	public CompletableFuture<GameMap> open(MinecraftServer server) {
		Holder<DimensionType> dimensionType = this.dimensionType.orElse(server.overworld().dimensionTypeRegistration());
		LevelStem dimension = new LevelStem(dimensionType, new VoidChunkGenerator(server));
		MapWorldSettings worldSettings = new MapWorldSettings();

		MapWorldInfo worldInfo = MapWorldInfo.create(server, worldSettings);
		RuntimeDimensionConfig config = new RuntimeDimensionConfig(dimension, 0, worldInfo);

		return CompletableFuture.supplyAsync(() -> openDimension(server, config), server)
				.thenApplyAsync(handle -> loadMapInto(server, worldSettings, handle), Util.backgroundExecutor())
				.thenApplyAsync(pair -> {
					RuntimeDimensionHandle dimensionHandle = pair.getFirst();
					MapMetadata metadata = pair.getSecond();
					return new GameMap(name.orElse(null), dimensionHandle.asKey(), metadata.regions())
							.onClose(game -> dimensionHandle.delete());
				}, server);
	}

	private RuntimeDimensionHandle openDimension(MinecraftServer server, RuntimeDimensionConfig config) {
		RuntimeDimensions dimensions = RuntimeDimensions.get(server);
		if (dimension.isEmpty()) {
			return dimensions.openTemporary(config);
		}

		RuntimeDimensionHandle handle = dimensions.openTemporaryWithKey(dimension.get(), config);
		if (handle != null) {
			return handle;
		} else {
			throw new GameException(Component.literal("Dimension already loaded in '" + dimension.get() + "'"));
		}
	}

	private Pair<RuntimeDimensionHandle, MapMetadata> loadMapInto(MinecraftServer server, MapWorldSettings mapWorldSettings, RuntimeDimensionHandle handle) {
		ResourceLocation path = loadFrom.withPath(p -> "maps/" + p + ".zip");

		Optional<Resource> resource = server.getResourceManager().getResource(path);
		if (resource.isEmpty()) {
			throw new GameException(Component.literal("No map exists at '" + path + "'"));
		}

		try {
			try (MapExportReader reader = MapExportReader.open(resource.get().open())) {
				MapMetadata metadata = reader.loadInto(server, handle.asKey());
				mapWorldSettings.importFrom(metadata.settings());
				return Pair.of(handle, metadata);
			}
		} catch (IOException e) {
			throw new GameException(Component.literal("Failed to load map from '" + path + "'"), e);
		}
	}
}
