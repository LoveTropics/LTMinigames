package org.lovetropics.games.common.core.game.map;

import com.lovetropics.lib.BlockBox;
import com.mojang.serialization.Codec;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.GameRegions;

import java.util.Collection;

public record RegionPattern(String pattern) {
	public static final Codec<RegionPattern> CODEC = Codec.STRING.xmap(RegionPattern::new, p -> p.pattern);

	public Collection<BlockBox> get(GameRegions regions, Object... args) {
		return regions.get(resolveKey(args));
	}

	public BlockBox getOrThrow(GameRegions regions, Object... args) {
		return regions.getOrThrow(resolveKey(args));
	}

	public @Nullable BlockBox getAny(GameRegions regions, Object... args) {
		return regions.getAny(resolveKey(args));
	}

	private String resolveKey(Object[] args) {
		return String.format(pattern, args);
	}
}
