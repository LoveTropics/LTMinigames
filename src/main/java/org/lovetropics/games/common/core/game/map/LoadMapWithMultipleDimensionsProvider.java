package org.lovetropics.games.common.core.game.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelData;
import org.apache.commons.lang3.mutable.MutableObject;
import org.lovetropics.dimensions.LinkedDimensions;
import org.lovetropics.dimensions.RuntimeDimensionConfig;
import org.lovetropics.dimensions.RuntimeDimensionHandle;
import org.lovetropics.dimensions.RuntimeDimensions;
import org.lovetropics.dimensions.SharedDimensionState;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.util.world.VoidChunkGenerator;
import org.lovetropics.maps.MapExportReader;
import org.lovetropics.maps.MapMetadata;
import org.lovetropics.maps.MapRegions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public record LoadMapWithMultipleDimensionsProvider(
		List<MapDefinition> dimensions
) implements IGameMapProvider {
	public record MapDefinition(Identifier loadFrom, Holder<DimensionType> dimensionType, Optional<String> spawnRegion){
		public static final Codec<MapDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
				Identifier.CODEC.fieldOf("load_from").forGetter(c -> c.loadFrom),
				DimensionType.CODEC.fieldOf("dimension_type").forGetter(c -> c.dimensionType),
				Codec.STRING.optionalFieldOf("spawn_region").forGetter(c -> c.spawnRegion)
		).apply(i, MapDefinition::new));
	}
	public static final MapCodec<LoadMapWithMultipleDimensionsProvider> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			MapDefinition.CODEC.listOf().fieldOf("dimensions").forGetter(c -> c.dimensions)
	).apply(i, LoadMapWithMultipleDimensionsProvider::new));

	public record DimensionData(MapDefinition definition, MutableObject<MapMetadata> mapMetadata, CompletableFuture<RuntimeDimensionHandle> runtimeDimensionHandle) {}

	@Override
	public MapCodec<LoadMapWithMultipleDimensionsProvider> getCodec() {
		return CODEC;
	}

	@Override
	public List<ResourceKey<Level>> getPossibleDimensions() {
		return List.of();
	}

	@Override
	public CompletableFuture<GameMap> open(MinecraftServer server) {
		VoidChunkGenerator voidChunkGenerator = new VoidChunkGenerator(server);
		SharedDimensionState sharedState = SharedDimensionState.createFresh(server);

		Map<Identifier, DimensionData> awaitingLoad = new HashMap<>();
		this.dimensions.forEach(d -> {
			MutableObject<MapMetadata> metadataHolder = new MutableObject<>();
			awaitingLoad.put(d.loadFrom, new DimensionData(d, metadataHolder, openDimension(server, d.loadFrom(), RuntimeDimensions.get(server), d.dimensionType(), voidChunkGenerator, sharedState, metadataHolder)));
		});

		return CompletableFuture.allOf(awaitingLoad.values().stream().map(DimensionData::runtimeDimensionHandle).toArray(CompletableFuture[]::new))
				.thenApplyAsync(_ -> {
					Map<ResourceKey<Level>, MapRegions> mapRegionsMap = new LinkedHashMap<>();
					Map<ResourceKey<Level>, ResourceKey<Level>> byVanillaKey = new LinkedHashMap<>();
					List<RuntimeDimensionHandle> handles = new ArrayList<>();
					String overworldSpawnRegion = null;
					for (Identifier identifier : awaitingLoad.keySet()) {
						DimensionData dimensionData = awaitingLoad.get(identifier);
						CompletableFuture<RuntimeDimensionHandle> completableFuture = dimensionData.runtimeDimensionHandle();
						MapMetadata mapMetadata = dimensionData.mapMetadata().get();
						RuntimeDimensionHandle handle;
						try {
							handle = completableFuture.get();
							mapRegionsMap.put(handle.asKey(), mapMetadata.regions());
							handles.add(handle);
							if(dimensionData.definition().dimensionType().is(BuiltinDimensionTypes.OVERWORLD)){
								mapMetadata.settings().setupInto(server, sharedState);
								byVanillaKey.put(Level.OVERWORLD, handle.asKey());
								overworldSpawnRegion = dimensionData.definition().spawnRegion().get();
							} else if(dimensionData.definition().dimensionType().is(BuiltinDimensionTypes.NETHER)){
								byVanillaKey.put(Level.NETHER, handle.asKey());
							} else if(dimensionData.definition().dimensionType().is(BuiltinDimensionTypes.END)){
								byVanillaKey.put(Level.END, handle.asKey());
							}
						} catch (InterruptedException | ExecutionException e) {
							e.printStackTrace();
							throw new GameException(Component.literal("Failed to load map with id '" + identifier + "'"));						}
					}
					if(overworldSpawnRegion == null){
						throw new GameException(Component.literal("Spawn for overworld dimension not specified!"));
					}
					ResourceKey<Level> overworldKey = byVanillaKey.get(Level.OVERWORLD);
					BlockPos blockPos = new ArrayList<>(mapRegionsMap.get(overworldKey).get(overworldSpawnRegion)).getFirst().centerBlock();
					LevelData.RespawnData respawnData = LevelData.RespawnData.of(overworldKey, blockPos, 0.0f, 0.0f);
					RuntimeDimensions.get(server).link(new LinkedDimensions(byVanillaKey, respawnData));
					return new GameMap(mapRegionsMap.keySet().stream().toList(), mapRegionsMap, _ -> {
						handles.forEach(RuntimeDimensionHandle::markForDeletion);
					});
		}, server);
	}

	private CompletableFuture<RuntimeDimensionHandle> openDimension(MinecraftServer server, Identifier loadFrom, RuntimeDimensions dimensions,
												 Holder<DimensionType> dimensionType,
												 VoidChunkGenerator voidChunkGenerator,
												 SharedDimensionState sharedDimensionState, MutableObject<MapMetadata> metadataHolder) {
		LevelStem dimension = new LevelStem(dimensionType, voidChunkGenerator);
		RuntimeDimensionConfig config = new RuntimeDimensionConfig(dimension, sharedDimensionState);
		return dimensions.openTemporary(config, dimensionPath ->
				CompletableFuture.runAsync(
						() -> metadataHolder.setValue(loadMapInto(loadFrom, server, dimensionPath)),
						Util.backgroundExecutor()
				)
		);
	}

	private MapMetadata loadMapInto(Identifier loadFrom, MinecraftServer server, Path dimensionPath) {
		try {
			try (MapExportReader reader = MapExportReader.open(server, loadFrom)) {
				return reader.loadInto(dimensionPath);
			}
		} catch (IOException e) {
			throw new GameException(Component.literal("Failed to load map with id '" + loadFrom + "'" + e.getMessage()), e);
		}
	}
}
