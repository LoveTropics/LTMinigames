package org.lovetropics.games.common.core.game;

import com.lovetropics.lib.BlockBox;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface GameRegions {
	Set<String> keySet();

	Collection<BlockBox> get(String key);

	default @Nullable BlockBox getAny(String key) {
		Collection<BlockBox> regions = get(key);
		if (regions.isEmpty()) {
			return null;
		}
		return regions.iterator().next();
	}

	default List<BlockBox> getAll(String... keys) {
		return getAll(Arrays.asList(keys));
	}

	default List<BlockBox> getAll(Collection<String> keys) {
		return keys.stream()
				.flatMap(key -> get(key).stream())
				.toList();
	}

	default List<BlockBox> getAllOrThrow(String key) {
		List<BlockBox> region = getAll(key);
		if (region.isEmpty()) {
			throw new GameException(Component.literal("Missing expected region with key '" + key + "'"));
		}
		return region;
	}

	default BlockBox getOrThrow(String key) {
		BlockBox region = getAny(key);
		if (region == null) {
			throw new GameException(Component.literal("Missing expected region with key '" + key + "'"));
		}
		return region;
	}
}
