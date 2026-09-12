package com.lovetropics.minigames.common.core.game.map;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.dimension.LinkedDimensions;
import com.lovetropics.minigames.common.core.dimension.RuntimeDimensionConfig;
import com.lovetropics.minigames.common.core.dimension.RuntimeDimensionHandle;
import com.lovetropics.minigames.common.core.dimension.RuntimeDimensions;
import com.lovetropics.minigames.common.core.map.MapRegions;
import com.lovetropics.minigames.common.core.map.MapWorldInfo;
import com.lovetropics.minigames.common.core.map.MapWorldSettings;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.PlayerSpawnFinder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/// @param nether if present, a Nether generated alongside the main dimension, which Nether portals lead to
/// @param end    if present, an End generated alongside the main dimension, which End portals lead to
public record GeneratorMapProvider(
		Optional<String> name,
		ChunkGenerator generator,
		Holder<DimensionType> dimensionType,
		Optional<Long> seed,
		Optional<String> spawnRegion,
		Optional<ExtraDimension> nether,
		Optional<ExtraDimension> end
) implements IGameMapProvider {
	public static final MapCodec<GeneratorMapProvider> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.optionalFieldOf("name").forGetter(c -> c.name),
			ChunkGenerator.CODEC.fieldOf("generator").forGetter(c -> c.generator),
			DimensionType.CODEC.fieldOf("dimension").forGetter(c -> c.dimensionType),
			Codec.LONG.optionalFieldOf("seed").forGetter(GeneratorMapProvider::seed),
			Codec.STRING.optionalFieldOf("spawn_region").forGetter(GeneratorMapProvider::spawnRegion),
			ExtraDimension.CODEC.optionalFieldOf("nether").forGetter(GeneratorMapProvider::nether),
			ExtraDimension.CODEC.optionalFieldOf("end").forGetter(GeneratorMapProvider::end)
	).apply(i, GeneratorMapProvider::new));

	// Same area that vanilla searches when picking the overworld spawn
	private static final int SPAWN_SEARCH_RADIUS = 5;

	@Override
	public MapCodec<GeneratorMapProvider> getCodec() {
		return CODEC;
	}

	public GeneratorMapProvider withSeed(long seed) {
		return new GeneratorMapProvider(name, generator, dimensionType, Optional.of(seed), spawnRegion, nether, end);
	}

	@Override
	public CompletableFuture<GameMap> open(MinecraftServer server) {
		long seed = this.seed.orElseGet(() -> server.overworld().getRandom().nextLong());
		// Shared between all the dimensions, so that settings like difficulty apply to all of them
		MapWorldInfo worldInfo = MapWorldInfo.create(server, new MapWorldSettings());

		return CompletableFuture.supplyAsync(() -> openDimensions(server, seed, worldInfo), server)
				.thenCompose(dimensions -> createMap(server, dimensions)
						.whenComplete((map, throwable) -> {
							if (throwable != null) {
								dimensions.values().forEach(RuntimeDimensionHandle::delete);
							}
						})
				);
	}

	/// @return the opened dimensions keyed by the vanilla dimension that each stands in for
	private Map<ResourceKey<Level>, RuntimeDimensionHandle> openDimensions(MinecraftServer server, long seed, MapWorldInfo worldInfo) {
		RuntimeDimensions dimensions = RuntimeDimensions.get(server);
		Map<ResourceKey<Level>, RuntimeDimensionHandle> handles = new LinkedHashMap<>();
		handles.put(Level.OVERWORLD, openDimension(dimensions, generator, dimensionType, seed, worldInfo));
		nether.ifPresent(config -> handles.put(Level.NETHER, openDimension(dimensions, config.generator(), config.dimensionType(), seed, worldInfo)));
		end.ifPresent(config -> handles.put(Level.END, openDimension(dimensions, config.generator(), config.dimensionType(), seed, worldInfo)));
		return handles;
	}

	private static RuntimeDimensionHandle openDimension(RuntimeDimensions dimensions, ChunkGenerator generator, Holder<DimensionType> dimensionType, long seed, MapWorldInfo worldInfo) {
		LevelStem dimension = new LevelStem(dimensionType, generator, OptionalLong.of(seed));
		return dimensions.openTemporary(new RuntimeDimensionConfig(dimension, seed, worldInfo));
	}

	private CompletableFuture<GameMap> createMap(MinecraftServer server, Map<ResourceKey<Level>, RuntimeDimensionHandle> dimensions) {
		RuntimeDimensionHandle overwold = dimensions.get(Level.OVERWORLD);
		boolean linked = dimensions.size() > 1;
		// Linked dimensions also need a spawn point, for anything leaving the End to arrive at
		CompletableFuture<Optional<BlockPos>> spawnPos = spawnRegion.isPresent() || linked
				? findSpawnPos(overwold.asWorld()).thenApply(Optional::of)
				: CompletableFuture.completedFuture(Optional.empty());

		return spawnPos.thenApply(pos -> {
			MapRegions regions = new MapRegions();
			if (pos.isPresent() && spawnRegion.isPresent()) {
				regions.add(spawnRegion.get(), BlockBox.of(pos.get()));
			}

			GameMap map = new GameMap(name.orElse(null), overwold.asKey(), regions)
					.onClose(game -> dimensions.values().forEach(RuntimeDimensionHandle::delete));
			if (!linked) {
				return map;
			}

			Map<ResourceKey<Level>, ResourceKey<Level>> byVanillaKey = dimensions.entrySet().stream()
					.collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> entry.getValue().asKey()));
			LevelData.RespawnData respawnData = LevelData.RespawnData.of(overwold.asKey(), pos.orElseThrow(), 0.0f, 0.0f);
			RuntimeDimensions.get(server).	link(new LinkedDimensions(byVanillaKey, respawnData));

			// we can just put every dimension we have for consistency
			return map.withLinkedDimensions(dimensions.entrySet().stream()
					.filter(entry -> entry.getKey() != Level.OVERWORLD)
					.map(entry -> entry.getValue().asKey())
					.toList()
			);
		});
	}

	/// Finds a safe surface position near the world's natural spawn point, checking chunks in the same order as vanilla.
	/// Chunks are loaded asynchronously so that the server does not stall while their terrain generates.
	private static CompletableFuture<BlockPos> findSpawnPos(ServerLevel level) {
		ChunkPos origin = ChunkPos.containing(level.getChunkSource().randomState().sampler().findSpawnPosition());
		return findSpawnPos(level, spiralAround(origin, SPAWN_SEARCH_RADIUS).iterator(), origin);
	}

	private static CompletableFuture<BlockPos> findSpawnPos(ServerLevel level, Iterator<ChunkPos> candidates, ChunkPos origin) {
		if (!candidates.hasNext()) {
			// Nothing suitable nearby (e.g. all ocean), so just stand on top of whatever is at the origin
			return loadChunk(level, origin).thenApplyAsync(
					unused -> level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, origin.getMiddleBlockPosition(0)),
					level.getServer()
			);
		}
		ChunkPos chunkPos = candidates.next();
		return loadChunk(level, chunkPos).thenComposeAsync(unused -> {
			BlockPos pos = PlayerSpawnFinder.getSpawnPosInChunk(level, chunkPos);
			return pos != null ? CompletableFuture.completedFuture(pos) : findSpawnPos(level, candidates, origin);
		}, level.getServer());
	}

	private static CompletableFuture<?> loadChunk(ServerLevel level, ChunkPos pos) {
		return level.getChunkSource().addTicketAndLoadWithRadius(TicketType.SPAWN_SEARCH, pos, 0);
	}

	private static List<ChunkPos> spiralAround(ChunkPos origin, int radius) {
		int size = radius * 2 + 1;
		List<ChunkPos> chunks = new ArrayList<>(size * size);
		int x = 0;
		int z = 0;
		int dx = 0;
		int dz = -1;
		for (int i = 0; i < size * size; i++) {
			chunks.add(new ChunkPos(origin.x() + x, origin.z() + z));
			if (x == z || (x < 0 && x == -z) || (x > 0 && x == 1 - z)) {
				int turn = dx;
				dx = -dz;
				dz = turn;
			}
			x += dx;
			z += dz;
		}
		return chunks;
	}

	public record ExtraDimension(ChunkGenerator generator, Holder<DimensionType> dimensionType) {
		public static final Codec<ExtraDimension> CODEC = RecordCodecBuilder.create(i -> i.group(
				ChunkGenerator.CODEC.fieldOf("generator").forGetter(ExtraDimension::generator),
				DimensionType.CODEC.fieldOf("dimension").forGetter(ExtraDimension::dimensionType)
		).apply(i, ExtraDimension::new));
	}
}
