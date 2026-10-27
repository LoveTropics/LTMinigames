package org.lovetropics.games.common.core.game.region;

import com.lovetropics.lib.BlockBox;

import java.util.Collection;
import java.util.List;

public interface GameRegions {
	GameRegions EMPTY = _ -> List.of();

	Collection<BlockBox> get(String key);
}
