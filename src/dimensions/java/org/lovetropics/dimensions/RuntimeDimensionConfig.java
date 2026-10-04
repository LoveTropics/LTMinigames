package org.lovetropics.dimensions;

import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.ServerLevelData;

public record RuntimeDimensionConfig(
		LevelStem levelStem,
		ServerLevelData worldInfo
) {
}
