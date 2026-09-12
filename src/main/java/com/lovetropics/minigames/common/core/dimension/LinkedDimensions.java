package com.lovetropics.minigames.common.core.dimension;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

/// A group of runtime dimensions that stand in for the vanilla Overworld, Nether and End, so that portals lead between them
/// rather than into the server's real dimensions.
///
/// @param byVanillaKey the dimension that stands in for each vanilla dimension
/// @param respawnData where anything leaving the End of this group arrives, in place of the server's spawn
public record LinkedDimensions(
		Map<ResourceKey<Level>, ResourceKey<Level>> byVanillaKey,
		LevelData.RespawnData respawnData
) {
	public Collection<ResourceKey<Level>> dimensions() {
		return byVanillaKey.values();
	}

	public boolean contains(ResourceKey<Level> dimension) {
		return byVanillaKey.containsValue(dimension);
	}

	public static @Nullable LinkedDimensions get(Level level) {
		if (level instanceof ServerLevel serverLevel) {
			RuntimeDimensions dimensions = RuntimeDimensions.getOrNull(serverLevel.getServer());
			if (dimensions != null) {
				return dimensions.getLinks(level.dimension());
			}
		}
		return null;
	}

	/// @return the vanilla dimension that the given level stands in for, or its own dimension if it is not linked
	public static ResourceKey<Level> vanillaKey(Level level) {
		LinkedDimensions links = get(level);
		if (links != null) {
			for (Map.Entry<ResourceKey<Level>, ResourceKey<Level>> entry : links.byVanillaKey.entrySet()) {
				if (entry.getValue() == level.dimension()) {
					return entry.getKey();
				}
			}
		}
		return level.dimension();
	}

	/// @return the dimension to travel to from the given level, when vanilla would travel to the given dimension: its stand-in if the
	/// level is linked, or `null` if nothing in the group stands in for it - linked dimensions never lead outside their group
	public static @Nullable ResourceKey<Level> resolve(Level from, ResourceKey<Level> dimension) {
		LinkedDimensions links = get(from);
		if (links == null || links.contains(dimension)) {
			return dimension;
		}
		return links.byVanillaKey.get(dimension);
	}
}
