package org.lovetropics.games.common.core.game.map;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.maps.MapRegions;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/// @param dimensions dimensions that are part of this map alongside its main one, e.g. its own Nether and End
public record GameMap(
		List<ResourceKey<Level>> dimensions,
		Map<ResourceKey<Level>, MapRegions> mapRegions,
		@Nullable Consumer<IGamePhase> close
) {
	public GameMap(ResourceKey<Level> dimension, Map<ResourceKey<Level>, MapRegions> mapRegions, @Nullable Consumer<IGamePhase> close) {
		this(List.of(dimension), mapRegions, close);
	}

	public GameMap(List<ResourceKey<Level>> dimensions, Map<ResourceKey<Level>,MapRegions> mapRegions) {
		this(dimensions, mapRegions, null);
	}

	public GameMap(ResourceKey<Level> dimension, MapRegions mapRegions) {
		this(dimension, Map.of(dimension, mapRegions), null);
	}

	public GameMap(ResourceKey<Level> dimension) {
		this(dimension, new MapRegions());
	}


	public GameMap onClose(Consumer<IGamePhase> close) {
		return new GameMap(dimensions, mapRegions, close);
	}

	/// @return the main dimension of this map, followed by any linked to it
	public List<ResourceKey<Level>> allDimensions() {
		return dimensions;
	}

	public void close(IGamePhase game) {
		if (close != null) {
			close.accept(game);
		}
	}
}
