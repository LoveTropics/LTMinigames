package com.lovetropics.minigames.common.core.game.map;

import com.lovetropics.minigames.common.core.game.GameException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public record InlineMapProvider(ResourceKey<Level> dimension) implements IGameMapProvider {
	public static final MapCodec<InlineMapProvider> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(c -> c.dimension)
	).apply(i, InlineMapProvider::new));

	@Override
	public MapCodec<InlineMapProvider> getCodec() {
		return CODEC;
	}

	@Override
	public List<ResourceKey<Level>> getPossibleDimensions() {
		return Collections.singletonList(dimension);
	}

	@Override
	public CompletableFuture<GameMap> open(MinecraftServer server) {
		if (server.getLevel(dimension) == null) {
			return CompletableFuture.failedFuture(new GameException(Component.literal("Missing dimension " + dimension)));
		}

		GameMap map = new GameMap(null, dimension);
		return CompletableFuture.completedFuture(map);
	}
}
