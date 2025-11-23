package com.lovetropics.minigames.common.core.game.map;

import com.lovetropics.minigames.common.core.dimension.RuntimeDimensionConfig;
import com.lovetropics.minigames.common.core.dimension.RuntimeDimensionHandle;
import com.lovetropics.minigames.common.core.dimension.RuntimeDimensions;
import com.lovetropics.minigames.common.core.map.MapRegions;
import com.lovetropics.minigames.common.core.map.MapWorldInfo;
import com.lovetropics.minigames.common.core.map.MapWorldSettings;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public record GeneratorMapProvider(
		Optional<String> name,
		ChunkGenerator generator,
		Holder<DimensionType> dimensionType,
		Optional<Long> seed
) implements IGameMapProvider {
	public static final MapCodec<GeneratorMapProvider> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.optionalFieldOf("name").forGetter(c -> c.name),
			ChunkGenerator.CODEC.fieldOf("generator").forGetter(c -> c.generator),
			DimensionType.CODEC.fieldOf("dimension").forGetter(c -> c.dimensionType),
			Codec.LONG.optionalFieldOf("seed").forGetter(GeneratorMapProvider::seed)
	).apply(i, GeneratorMapProvider::new));

	@Override
	public MapCodec<GeneratorMapProvider> getCodec() {
		return CODEC;
	}

	@Override
	public CompletableFuture<GameMap> open(MinecraftServer server) {
		LevelStem dimension = new LevelStem(dimensionType, generator);

		MapWorldInfo worldInfo = MapWorldInfo.create(server, new MapWorldSettings());
		RuntimeDimensionConfig config = new RuntimeDimensionConfig(dimension, seed.orElseGet(() -> server.overworld().random.nextLong()), worldInfo);

		return CompletableFuture.supplyAsync(() -> {
			RuntimeDimensionHandle dimensionHandle = RuntimeDimensions.get(server).openTemporary(config);
			return new GameMap(name.orElse(null), dimensionHandle.asKey(), new MapRegions())
					.onClose(game -> dimensionHandle.delete());
		}, server);
	}
}
