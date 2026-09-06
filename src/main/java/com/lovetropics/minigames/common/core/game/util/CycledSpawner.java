package com.lovetropics.minigames.common.core.game.util;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.map.MapRegions;

import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CycledSpawner {

	public static final CycledSpawner EMPTY = new CycledSpawner(List.of());

	private final List<BlockBox> regions;
	private int index;

	public CycledSpawner(List<BlockBox> regions) {
		this.regions = new ArrayList<>(regions);
		Collections.shuffle(this.regions);
	}

	public CycledSpawner(MapRegions regions, String... keys) {
		this(regions.getAll(keys));
	}

	public List<BlockBox> regions() {
		return regions;
	}

	public @Nullable BlockBox next() {
		if (regions.isEmpty()) {
			return null;
		}
		return regions.get(index++ % regions.size());
	}

	public CycledSpawner take(int count) {
		List<BlockBox> result = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			BlockBox region = next();
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
