package org.lovetropics.games.common.core.game.util;

import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.region.Region;
import org.lovetropics.games.common.core.game.region.RegionQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CycledSpawner {

	public static final CycledSpawner EMPTY = new CycledSpawner(List.of());

	private final List<Region> regions;
	private int index;

	public CycledSpawner(List<Region> regions) {
		this.regions = new ArrayList<>(regions);
		Collections.shuffle(this.regions);
	}

	public CycledSpawner(IGamePhase game, RegionQuery query) {
		this(query.all(game));
	}

	public List<Region> regions() {
		return regions;
	}

	public @Nullable Region next() {
		if (regions.isEmpty()) {
			return null;
		}
		return regions.get(index++ % regions.size());
	}

	public CycledSpawner take(int count) {
		List<Region> result = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			Region region = next();
			if (region == null) {
				break;
			}
			result.add(region);
		}
		return new CycledSpawner(result);
	}

	public int size() {
		return regions.size();
	}
}
