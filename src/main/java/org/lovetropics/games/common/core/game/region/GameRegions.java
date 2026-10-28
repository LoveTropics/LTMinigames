package org.lovetropics.games.common.core.game.region;

import java.util.Collection;
import java.util.List;

public interface GameRegions {
	GameRegions EMPTY = _ -> List.of();

	Collection<Region> get(String key);
}
