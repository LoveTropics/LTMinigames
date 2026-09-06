package com.lovetropics.minigames.common.core.game.map;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.map.MapRegions;
import com.mojang.serialization.Codec;

import org.jspecify.annotations.Nullable;
import java.util.Collection;

public record RegionPattern(String pattern) {
	public static final Codec<RegionPattern> CODEC = Codec.STRING.xmap(RegionPattern::new, p -> p.pattern);

	public Collection<BlockBox> get(MapRegions regions, Object... args) {
		return regions.get(resolveKey(args));
	}

	public BlockBox getOrThrow(MapRegions regions, Object... args) {
		return regions.getOrThrow(resolveKey(args));
	}

	public @Nullable BlockBox getAny(MapRegions regions, Object... args) {
		return regions.getAny(resolveKey(args));
	}

	private String resolveKey(Object[] args) {
		return String.format(pattern, args);
	}
}
