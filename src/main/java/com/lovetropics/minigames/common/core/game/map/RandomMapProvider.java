package com.lovetropics.minigames.common.core.game.map;

import com.lovetropics.lib.codec.MoreCodecs;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Util;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public record RandomMapProvider(IGameMapProvider[] mapProviders) implements IGameMapProvider {
	public static final MapCodec<RandomMapProvider> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			MoreCodecs.arrayOrUnit(GameMapProviders.CODEC, IGameMapProvider[]::new).fieldOf("pool").forGetter(c -> c.mapProviders)
	).apply(i, RandomMapProvider::new));

	private static final RandomSource RANDOM = RandomSource.create();

	@Override
	public MapCodec<RandomMapProvider> getCodec() {
		return CODEC;
	}

	@Override
	public List<ResourceKey<Level>> getPossibleDimensions() {
		return Arrays.stream(mapProviders)
				.map(IGameMapProvider::getPossibleDimensions)
				.flatMap(List::stream)
				.collect(Collectors.toList());
	}

	@Override
	public CompletableFuture<GameMap> open(MinecraftServer server) {
		return Util.getRandom(mapProviders, RANDOM).open(server);
	}
}
