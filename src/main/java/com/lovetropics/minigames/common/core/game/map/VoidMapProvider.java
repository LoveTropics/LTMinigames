package com.lovetropics.minigames.common.core.game.map;

import com.lovetropics.minigames.common.core.dimension.RuntimeDimensionConfig;
import com.lovetropics.minigames.common.core.dimension.RuntimeDimensionHandle;
import com.lovetropics.minigames.common.core.dimension.RuntimeDimensions;
import com.lovetropics.minigames.common.core.map.MapRegions;
import com.lovetropics.minigames.common.core.map.MapWorldInfo;
import com.lovetropics.minigames.common.core.map.MapWorldSettings;
import com.lovetropics.minigames.common.core.map.VoidChunkGenerator;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public record VoidMapProvider(Optional<String> name, Optional<Holder<DimensionType>> dimensionType) implements IGameMapProvider {
	public static final MapCodec<VoidMapProvider> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.optionalFieldOf("name").forGetter(c -> c.name),
			DimensionType.CODEC.optionalFieldOf("dimension").forGetter(c -> c.dimensionType)
	).apply(i, VoidMapProvider::new));

	@Override
	public MapCodec<VoidMapProvider> getCodec() {
		return CODEC;
	}

	@Override
	public CompletableFuture<GameMap> open(MinecraftServer server) {
		Holder<DimensionType> dimensionType = this.dimensionType.orElse(server.overworld().dimensionTypeRegistration());
		LevelStem dimension = new LevelStem(dimensionType, new VoidChunkGenerator(server));

		MapWorldInfo worldInfo = MapWorldInfo.create(server, new MapWorldSettings());
		RuntimeDimensionConfig config = new RuntimeDimensionConfig(dimension, 0, worldInfo);

		return CompletableFuture.supplyAsync(() -> {
			RuntimeDimensionHandle dimensionHandle = RuntimeDimensions.get(server).openTemporary(config);
			return new GameMap(name.orElse(null), dimensionHandle.asKey(), new MapRegions())
					.onClose(game -> dimensionHandle.delete());
		}, server);
	}
}
