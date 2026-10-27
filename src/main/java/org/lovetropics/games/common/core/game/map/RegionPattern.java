package org.lovetropics.games.common.core.game.map;

import com.mojang.serialization.Codec;
import org.lovetropics.games.common.core.game.region.RegionQuery;

public record RegionPattern(String pattern) {
	public static final Codec<RegionPattern> CODEC = Codec.STRING.xmap(RegionPattern::new, p -> p.pattern);

	public RegionQuery resolve(Object... args) {
		return RegionQuery.of(String.format(pattern, args));
	}
}
