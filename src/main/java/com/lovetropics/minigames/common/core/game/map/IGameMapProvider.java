package com.lovetropics.minigames.common.core.game.map;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface IGameMapProvider {
	MapCodec<? extends IGameMapProvider> getCodec();

	CompletableFuture<GameMap> open(MinecraftServer server);

	default List<ResourceKey<Level>> getPossibleDimensions() {
		return Collections.emptyList();
	}
}
