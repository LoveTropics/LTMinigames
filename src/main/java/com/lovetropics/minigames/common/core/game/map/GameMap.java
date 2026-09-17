package com.lovetropics.minigames.common.core.game.map;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.map.MapRegions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import org.jspecify.annotations.Nullable;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/// @param linkedDimensions dimensions that are part of this map alongside its main one, e.g. its own Nether and End
public record GameMap(
		@Nullable String name,
		ResourceKey<Level> dimension,
		List<ResourceKey<Level>> linkedDimensions,
		MapRegions mapRegions,
		@Nullable Consumer<IGamePhase> close
) {
	public GameMap(@Nullable String name, ResourceKey<Level> dimension, MapRegions mapRegions, @Nullable Consumer<IGamePhase> close) {
		this(name, dimension, List.of(), mapRegions, close);
	}

	public GameMap(@Nullable String name, ResourceKey<Level> dimension, MapRegions mapRegions) {
		this(name, dimension, mapRegions, null);
	}

	public GameMap(@Nullable String name, ResourceKey<Level> dimension) {
		this(name, dimension, new MapRegions());
	}

	public GameMap withName(String key) {
		return new GameMap(key, dimension, linkedDimensions, mapRegions, close);
	}

	public GameMap withLinkedDimensions(List<ResourceKey<Level>> linkedDimensions) {
		return new GameMap(name, dimension, linkedDimensions, mapRegions, close);
	}

	public GameMap onClose(Consumer<IGamePhase> close) {
		return new GameMap(name, dimension, linkedDimensions, mapRegions, close);
	}

	/// @return the main dimension of this map, followed by any linked to it
	public List<ResourceKey<Level>> allDimensions() {
		return Stream.concat(Stream.of(dimension), linkedDimensions.stream()).toList();
	}

	public void close(IGamePhase game) {
		if (close != null) {
			close.accept(game);
		}
	}
}
