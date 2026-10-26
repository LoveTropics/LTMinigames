package org.lovetropics.games.common.core.game.map;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.maps.MapRegions;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/// @param linkedDimensions dimensions that are part of this map alongside its main one, e.g. its own Nether and End
public record GameMap(
		ResourceKey<Level> dimension,
		List<ResourceKey<Level>> linkedDimensions,
		MapRegions mapRegions,
		@Nullable Consumer<IGamePhase> close
) {
	public GameMap(ResourceKey<Level> dimension, MapRegions mapRegions, @Nullable Consumer<IGamePhase> close) {
		this(dimension, List.of(), mapRegions, close);
	}

	public GameMap(ResourceKey<Level> dimension, MapRegions mapRegions) {
		this(dimension, mapRegions, null);
	}

	public GameMap(ResourceKey<Level> dimension) {
		this(dimension, new MapRegions());
	}

	public GameMap withLinkedDimensions(List<ResourceKey<Level>> linkedDimensions) {
		return new GameMap(dimension, linkedDimensions, mapRegions, close);
	}

	public GameMap onClose(Consumer<IGamePhase> close) {
		return new GameMap(dimension, linkedDimensions, mapRegions, close);
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
